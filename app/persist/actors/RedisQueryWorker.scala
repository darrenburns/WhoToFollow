package persist.actors

import akka.actor.{Props, ActorRef, Actor}
import channels.actors.UserChannelSupervisor
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import com.google.inject.name.Named
import com.redis.RedisClient.DESC
import hooks.RedisConnectionPool
import learn.actors.FeatureExtraction.UserFeatures
import play.api.Logger
import channels.actors.MetricsReporting.RecentQueries

import scala.collection.mutable.ListBuffer

object RedisQueryWorker {
  sealed trait RedisQuery
  case class GetUserFeatures(screenName: String) extends RedisQuery
  case class SendUserFeaturesToChannel(screenName: String) extends RedisQuery
  case class HasStatusBeenProcessed(status: twitter4j.Status) extends RedisQuery
  case object GetRecentQueryList extends RedisQuery

  def extractFeatureCount(map: Map[String, String], featureName: String): Int = {
    map.get(featureName) match {
      case Some(count) => count.toInt
      case None => 0
    }
  }

  def props(channelSupervisor: ActorRef, metricsChannel: ActorRef): Props =
    Props(new RedisQueryWorker(channelSupervisor, metricsChannel))
}

/**
  * RedisReaders read take requests to fetch data from Redis and send it along
  * to wherever it's required. The set of potential destinations are passed as arguments.
  */
class RedisQueryWorker(userChannel: ActorRef, metricsReporting: ActorRef) extends Actor {

  import RedisQueryWorker._

  private val clients = RedisConnectionPool.pool

  override def receive = {

    case GetRecentQueryList =>
      val recentQueryResult = clients.withClient{client =>
        client.lrange("recentQueries", 0, 19)
      }
      var resultBatch = ListBuffer[String]()
      recentQueryResult match {
        case Some(queryResults) =>
          queryResults.foreach {
            case Some(r) =>
              resultBatch += r
          }
      }
      metricsReporting ! RecentQueries(resultBatch.toList)

    case GetUserFeatures(screenName) =>
      Logger.debug(s"RedisQueryWorker [${self.path}] processing feature request for user '$screenName'.")
      sender ! getUserFeatures(screenName)

    case SendUserFeaturesToChannel(screenName) =>
      userChannel ! getUserFeatures(screenName)

    case HasStatusBeenProcessed(status) =>
      clients.withClient{client =>
        val statusId = status.getId
        val hasBeenProcessed = client.sismember(s"user:${status.getUser.getScreenName}:tweetIds", statusId)
        sender ! (statusId, hasBeenProcessed)
      }

  }

  private def getUserFeatures(screenName: String): UserFeatures = {
    val userStats = clients.withClient{client =>
      client.hgetall(s"user:$screenName:stats")
    }

    val hashtagTimestamps = clients.withClient{client =>
      client.zrangeWithScore(s"user:$screenName:timestamps", 0, -1, DESC)
    }

    userStats match {
      case Some(userFeatureMap) =>
        UserFeatures(
          screenName=screenName,
          tweetCount=extractFeatureCount(userFeatureMap, "tweetCount"),
          followerCount=extractFeatureCount(userFeatureMap, "followerCount"),
          wordCount=extractFeatureCount(userFeatureMap, "wordCount"),
          capitalisedCount=extractFeatureCount(userFeatureMap, "capitalisedCount"),
          hashtagCount=extractFeatureCount(userFeatureMap, "hashtagCount"),
          retweetCount=extractFeatureCount(userFeatureMap, "retweetCount"),
          likeCount=extractFeatureCount(userFeatureMap, "likeCount"),
          dictionaryHits=extractFeatureCount(userFeatureMap, "dictionaryHits"),
          linkCount=extractFeatureCount(userFeatureMap, "linkCount"),
          hashtagTimestamps=hashtagTimestamps match {
            case Some(timestampList) => timestampList
            case None => List.empty[(String, Double)]
          }
        )
    }
  }

}
