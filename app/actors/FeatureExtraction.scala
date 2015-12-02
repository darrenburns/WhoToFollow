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
    /**
      * The width in seconds of the sliding window over the incoming tweets.
      * The window has WindowSize seconds width and also slides in strides
      * of WindowSize seconds, to prevent overlap and therefore duplicated processing
      * of tweets.
      */
    val WindowSize = 10
  }

  /**
    * Representation of features which can be extracted from a tweet.
    *
    * @param username The user's username (not including the '@')
    * @param followerCount The number of followers the user has
    * @param punctuationCounts Totals for each punctuation character in the tweet
    * @param wordCount The number of words in the tweet
    * @param capWordCount The number of capitalised words in the tweet
    * @param hashtagCount The number of hashtags used in the tweet
    * @param retweetCount The number of retweets the tweet has received
    * @param mentionCount The number of different users mentioned in the tweet
    * @param likeCount The number of likes the tweet has received
    * @param dictionaryHits The number of words that are spelled correctly (found in dictionary)
    * @param linkCount The number of links contained within the tweet
    */
  case class TweetFeatures(username: String,
                           followerCount: Int,
                           punctuationCounts: Map[Char, Int],
                           wordCount: Int,
                           capWordCount: Int,
                           hashtagCount: Int,
                           retweetCount: Int,
                           mentionCount: Int,
                           likeCount: Int,
                           dictionaryHits: Int,
                           linkCount: Int
                          )
  case class CheckQuality(status: Status)
}

/**
  * Receives a stream handle from TweetStreamActor and assigns initial
  * tweet quality ratings to the associated users.
  *
  * This class must be a Singleton because it contains a Spark Streaming
  * actions. After the Spark context is initialised, we cannot add any
  * further actions. A non-singleton actor would result in attempts
  * to add new Spark actions every time it is injected. Marking an
  * actor as a Singleton ensures Guice will only inject it once, therefore
  * removing the possibility of attempts at adding new actions.
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
      Logger.info("FeatureExtraction starting.")
      findStreamFeatures(stream)
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

    // Send report batches for writing in Redis
    tweetQualityReports.window(Seconds(windowSize), Seconds(windowSize)).foreachRDD(report => {
      redisWriter ! TweetQualityReportBatch(report.collect())
    })

    // Indicate readiness to recipient actor TweetStreamActor
    Logger.info("FeatureExtraction ready.")
    sender ! Ready()

  }

}

