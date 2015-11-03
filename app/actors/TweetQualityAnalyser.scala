package actors

import actors.RedisWriter.TweetQualityReportBatch
import actors.TweetQualityAnalyser.{TweetQualityReport, CheckQuality}
import actors.UserHashtagCounter.ActiveTwitterStream
import akka.actor.{ActorRef, Actor}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import play.api.Configuration
import twitter4j.Status
import utils.QualityAnalysisSupport


object TweetQualityAnalyser {

  object Defaults {
    val WindowSize = 10
  }

  case class TweetQualityReport(username: String, followerCount: Int, punctuationCounts: Map[Char, Int],
                                wordCount: Int, capWordCount: Int, hashtagCount: Int, retweetCount: Long,
                                mentionCount: Int)
  case class CheckQuality(status: Status)
}

/** Receives a stream handle from TweetStreamActor and assigns initial
  * tweet quality ratings to the associated users.
  */
@Singleton
class TweetQualityAnalyser @Inject()
  (@Named("redisWriter") redisWriter: ActorRef, configuration: Configuration)
  extends Actor with QualityAnalysisSupport {

  import TweetQualityAnalyser._

  val windowSize = configuration.getInt("analysis.featureExtraction.windowSize").getOrElse(Defaults.WindowSize)

  override def receive = {
    /*
    Initial basic feature extraction.
     */
    case ActiveTwitterStream(stream) =>
      val tweetQualityReports = stream.map(tweet =>
        TweetQualityReport(
          username = tweet.getUser.getScreenName,
          followerCount = tweet.getUser.getFollowersCount,
          punctuationCounts = getPunctuationCounts(tweet.getText),
          wordCount = countWords(tweet.getText),
          capWordCount = countCapitalisedWords(tweet.getText),
          hashtagCount = tweet.getHashtagEntities.length,
          retweetCount = tweet.getRetweetCount,
          mentionCount = tweet.getUserMentionEntities.length
        )
      )
      tweetQualityReports.window(Seconds(windowSize), Seconds(windowSize))
      tweetQualityReports.foreachRDD(report => {
        redisWriter ! TweetQualityReportBatch(report.toLocalIterator.toSeq)
      })
  }

}

