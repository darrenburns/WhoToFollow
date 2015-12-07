package learn.actors

import akka.actor.{Actor, ActorRef}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import learn.actors.TweetStreamActor.Ready
import learn.actors.UserHashtagCounter.ActiveTwitterStream
import learn.utility.ExtractionUtils._
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.dstream.{DStream, ReceiverInputDStream}
import persist.actors.RedisWriter
import RedisWriter.TweetQualityReportBatch
import play.api.{Configuration, Logger}
import twitter4j.Status

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
(
  @Named("redisWriter") redisWriter: ActorRef,
  configuration: Configuration
) extends Actor with Serializable {

  import FeatureExtraction._

  val WindowSize = configuration.getInt("analysis.featureExtraction.windowSize").getOrElse(Defaults.WindowSize)

  override def receive = {
    /*
    Initial basic feature extraction.
     */
    case ActiveTwitterStream(stream) =>
      Logger.info("FeatureExtraction starting...")
      mapToWindowedFeatureStream(stream, WindowSize).foreachRDD(features => {
          redisWriter ! TweetQualityReportBatch(features.collect())
      })
      sender ! Ready()
      Logger.info("FeatureExtraction is ready.")
  }


  private def mapToWindowedFeatureStream(stream: ReceiverInputDStream[Status], windowSize: Int): DStream[TweetFeatures]
    = stream.map(getStatusFeatures).window(Seconds(windowSize), Seconds(windowSize))

}
