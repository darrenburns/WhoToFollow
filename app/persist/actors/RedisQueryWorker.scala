package persist.actors

import akka.actor.Actor
import com.redis.RedisClient.DESC
import hooks.RedisConnectionPool
import learn.actors.FeatureExtraction.UserFeatures
import play.api.Logger
import report.actors.MetricsReporting.RecentQueries

import scala.collection.mutable.ListBuffer

object RedisQueryWorker {
  sealed trait RedisQuery
  case class UserFeatureRequest(screenName: String) extends RedisQuery
  case class HasStatusBeenProcessed(status: twitter4j.Status) extends RedisQuery
  case object GetRecentQueryList extends RedisQuery

  def extractFeatureCount(map: Map[String, String], featureName: String): Int = {
    map.get(featureName) match {
      case Some(count) => count.toInt
      case None => 0
    }
  }
}

/**
  * RedisReaders read take requests to fetch data from Redis and send that data along
  * to wherever it's required.
  */
class RedisQueryWorker extends Actor {

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
      sender ! RecentQueries(resultBatch.toList)

    case UserFeatureRequest(screenName) =>
      val userStats = clients.withClient{client =>
        client.hgetall(s"user:$screenName:stats")
      }

      val hashtagTimestamps = clients.withClient{client =>
        client.zrangeWithScore(s"user:$screenName:timestamps", 0, -1, DESC)
      }

      userStats match {
        case Some(userFeatureMap) =>
          sender ! UserFeatures(
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
        case None => Logger.debug("Queried a non-existent user in Redis.")
      }

    case HasStatusBeenProcessed(status) =>
      clients.withClient{client =>
        val statusId = status.getId
        // Respond with a true or false
        sender ! (statusId, client.sismember(s"user:${status.getUser.getScreenName}:tweetIds", statusId))
      }

  }

}
