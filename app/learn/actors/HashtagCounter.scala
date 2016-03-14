package learn.actors

import akka.actor.{Actor, ActorRef}
import channels.actors.MetricsReporting
import channels.actors.MetricsReporting.TrendingHashtags
import com.fasterxml.jackson.databind.JsonNode
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import di.NamedActor
import learn.actors.TweetStreamActor.PipelineActorReady
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.slf4j.LoggerFactory
import persist.actors.RedisActor
import persist.actors.RedisWriterWorker.{UserHashtagReport, RedisWriteRequest}
import play.api.libs.json.{Json, Writes}
import play.api.{Configuration, Logger}
import play.mvc.WebSocket
import twitter4j.Status

import scala.language.postfixOps


object HashtagCounter extends NamedActor {
  override final val name = "HashtagCounter"

  object Defaults {
    val HashtagCountWindowSize = 10  // The number of seconds between each report back to supervisor
    val HashtagsPerBatch = 4
  }

  case class ActiveTwitterStream(dStream: ReceiverInputDStream[Status])
  case class ActivateOutputStream(out: WebSocket.Out[JsonNode])
  case class HashtagCount(hashtag: String, count: Int)

}

/**
  * Counts hashtags within a window. Sends reports to metrics channel.
 */

@Singleton
class HashtagCounter @Inject()
(
  @Named(MetricsReporting.name) metricsReporting: ActorRef,
  configuration: Configuration
) extends Actor {

  import HashtagCounter._


  override def receive = {
    case ActiveTwitterStream(stream) =>
      Logger.info("HashtagCounter starting.")
      processStream(stream)  // Initialise the counting of hashtags
      sender ! PipelineActorReady()
    case _ =>
      Logger.warn(s"${getClass.getName} received an unrecognised request.")
  }

  def processStream(stream: ReceiverInputDStream[Status]): Unit = {

    val reportFrequency = configuration.getInt("analysis.hashtagCount.windowSize")
      .getOrElse(Defaults.HashtagCountWindowSize)

    val numberOfHashtagsToShow = configuration.getInt("metrics.trendingHashtagsToShow")
      .getOrElse(Defaults.HashtagsPerBatch)

    // Map the tweets in the stream to a stream of (hashtag, 1) tuples
    val hashtags = stream flatMap(status => {
      status.getHashtagEntities map(hashtag => (hashtag.getText toLowerCase(), 1))
    })

    // Aggregate hashtags
    val hashtagCountInWindow = hashtags
      .reduceByKeyAndWindow(
        (p:Int, q:Int) => p+q, Seconds(reportFrequency), Seconds(reportFrequency)
      )
      .map{case (hashtag, count) => (count, hashtag)}
      .transform(_.sortByKey(ascending = false))
      .map{case (count, hashtag) => HashtagCount(hashtag, count)}

    // Send reports to Redis
    hashtagCountInWindow foreachRDD(rdd => {
      val hashtagCounts = rdd.collect take numberOfHashtagsToShow
      metricsReporting ! TrendingHashtags(hashtagCounts.toList)
    })

  }

}
