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
import utils.QualityAnalysisSupport._


object FeatureExtraction {

  object Defaults {
    val WindowSize = 10
  }

  case class TweetFeatures(username: String,
                           followerCount: Int,
                           punctuationCounts: Map[Char, Int],
                           wordCount: Int,
                           capWordCount: Int,
                           hashtagCount: Int,
                           retweetCount: Int,
                           mentionCount: Int,
                           likeCount: Int)
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
    Initial basic feature extraction. TODO: Extract features for spelling accuracy (using system dictionary)
     */
    case ActiveTwitterStream(stream) =>
      findStreamFeatures(stream)
      sender ! Ready()

  }

  def findStreamFeatures(stream: ReceiverInputDStream[Status]): Unit = {
    val tweetQualityReports = stream.map(status => {
      val tqr = TweetFeatures(
        username = status.getUser.getScreenName,
        followerCount = status.getUser.getFollowersCount,
        punctuationCounts = getPunctuationCounts(status.getText),
        wordCount = countWords(status.getText),
        capWordCount = countCapitalisedWords(status.getText),
        hashtagCount = status.getHashtagEntities.length,
        retweetCount = status.getRetweetCount,
        mentionCount = status.getUserMentionEntities.length,
        likeCount = status.getFavoriteCount
      )
      tqr
    })
    tweetQualityReports.window(Seconds(windowSize), Seconds(windowSize)).foreachRDD(report => {
      Logger.debug("Sending tweet quality report batch to redisWriter")
      redisWriter ! TweetQualityReportBatch(report.collect())
    })
  }

}

