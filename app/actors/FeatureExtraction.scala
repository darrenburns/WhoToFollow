package actors

import actors.RedisWriter.TweetQualityReportBatch
import actors.TweetStreamActor.Ready
import actors.UserHashtagCounter.ActiveTwitterStream
import akka.actor.{Actor, ActorRef}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import play.api.{Configuration, Logger}
import twitter4j.Status
import utils.QualityAnalyser


object FeatureExtraction {

  object Defaults {
    val WindowSize = 10
  }

  case class TweetFeatures(username: String,  // The user's username (not including the '@')
                           followerCount: Int,  // The number of followers the user has
                           punctuationCounts: Map[Char, Int],  // Totals for each punctuation character in the tweet
                           wordCount: Int,  // The number of words in the tweet
                           capWordCount: Int,  // The number of capitalised words in the tweet
                           hashtagCount: Int,  // The number of hashtags used in the tweet
                           retweetCount: Int,  // The number of retweets the tweet has received
                           mentionCount: Int,  // The number of different users mentioned in the tweet
                           likeCount: Int,  // The number of likes the tweet has received
                           dictionaryHits: Int,  // The number of words that are spelled correctly (found in dictionary)
                           linkCount: Int  // The number of links contained within the tweet
                          )
  case class CheckQuality(status: Status)
}

/** Receives a stream handle from TweetStreamActor and assigns initial
  * tweet quality ratings to the associated users.
  */
@Singleton
class FeatureExtraction @Inject()
  (@Named("redisWriter") redisWriter: ActorRef, configuration: Configuration)
  extends Actor with Serializable {

  import FeatureExtraction._

  val windowSize = configuration.getInt("analysis.featureExtraction.windowSize").getOrElse(Defaults.WindowSize)

  override def receive = {
    /*
    Initial basic feature extraction.
     */
    case ActiveTwitterStream(stream) =>
      findStreamFeatures(stream)
      sender ! Ready()
  }

  def findStreamFeatures(stream: ReceiverInputDStream[Status]): Unit = {
    val tweetQualityReports = stream.map(status => {
      val qa = new QualityAnalyser(status.getText)
      val htCount = status.getHashtagEntities.length
      val mentionCount = status.getUserMentionEntities.length
      val linkCount = status.getURLEntities.length
      val features = TweetFeatures(
        username = status.getUser.getScreenName,
        followerCount = status.getUser.getFollowersCount,
        punctuationCounts = qa.findPunctuationCounts(),
        wordCount = qa.countWords() - htCount - mentionCount - linkCount,
        capWordCount = qa.countCapitalisedWords(),
        hashtagCount = htCount,
        retweetCount = status.getRetweetCount,
        mentionCount = mentionCount,
        likeCount = status.getFavoriteCount,
        dictionaryHits = qa.countDictionaryHits(),
        linkCount = linkCount
      )
      features
    })
    tweetQualityReports.window(Seconds(windowSize), Seconds(windowSize)).foreachRDD(report => {
      Logger.debug("Sending tweet quality report batch to redisWriter")
      redisWriter ! TweetQualityReportBatch(report.collect())
    })
  }

}

