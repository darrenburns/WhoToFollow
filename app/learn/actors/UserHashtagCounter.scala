package learn.actors

import akka.actor.{Actor, ActorRef}
import com.fasterxml.jackson.databind.JsonNode
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
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


object UserHashtagCounter {
  val logger = LoggerFactory.getLogger(getClass)

  object Defaults {
    val HashtagCountWindowSize = 10  // The number of seconds between each report back to supervisor
  }

  implicit val hashtagCountWrites = new Writes[UserHashtagCount] {
    def writes(hashtagCount: UserHashtagCount) = Json.obj(
      "username" -> hashtagCount.username,
      "hashtag" -> hashtagCount.hashtag,
      "count" -> hashtagCount.count
    )
  }

  case class ActiveTwitterStream(dStream: ReceiverInputDStream[Status])
  case class ActivateOutputStream(out: WebSocket.Out[JsonNode])
  case class HashtagCount(hashtag: String, count: Int)
  case class UserHashtagCount(username: String, hashtag: String, count: Int)

}

/**
  * Counts hashtags on a per-user basis within a window.
  * Reports back to dispatcher every `Defaults.HashtagCountWindowSize` seconds.
 */

@Singleton
class UserHashtagCounter @Inject()
(
  @Named(RedisActor.name) redisActor: ActorRef,
  configuration: Configuration
) extends Actor {

  import UserHashtagCounter._

  override def receive = {
    case ActiveTwitterStream(stream) =>
      Logger.info("UserHashtagCounter starting.")
      processStream(stream)  // Initialise the counting of hashtags
    case _ =>
      Logger.warn(s"${getClass.getName} received an unrecognised request.")
  }

  def processStream(stream: ReceiverInputDStream[Status]): Unit = {

    // Get the report frequency configuration or use the default value
    val reportFrequency = configuration.getInt("analysis.hashtagCount.windowSize")
      .getOrElse(Defaults.HashtagCountWindowSize)

    // Status -> several tuples of form ((user, hashtag), 1)
    val userHashtags = stream.flatMap(status => {
      status.getHashtagEntities
        .map(hashtag => ((status.getUser.getScreenName, hashtag.getText toLowerCase()), 1))
    })

    // Counting aggregation of (user, hashtag) pairs
    val hashtagCountInWindow = userHashtags
      .reduceByKeyAndWindow((p:Int, q:Int) => p+q, Seconds(reportFrequency), Seconds(reportFrequency))
      .map{case ((user, hashtag), count) => (count, (user, hashtag))}
      .transform(_.sortByKey(ascending = false))
      .map{case (count, (user, hashtag)) => UserHashtagCount(user, hashtag, count)}

    // Send reports to Redis
    hashtagCountInWindow.foreachRDD(rdd => {
      val userHashtagCounts = rdd.collect()
      redisActor ! UserHashtagReport(userHashtagCounts)
    })

    Logger.debug("UserHashtagCounter ready.")
    sender ! PipelineActorReady()

  }

}
