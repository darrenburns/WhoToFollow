package persist.actors

import akka.actor.{ActorRef, Actor}
import com.google.inject.{Singleton, Inject}
import com.google.inject.name.Named
import com.redis.RedisClient.DESC
import hooks.RedisConnectionPool
import play.api.Logger
import report.actors.{ChannelManager, WebSocketSupervisor}
import ChannelManager.GetQueryMentionCounts
import report.actors.MetricsReporting.{GetLatestIndexSize, GetRecentQueryList, RecentQueries}
import report.actors.WebSocketSupervisor
import report.actors.WebSocketSupervisor.LatestIndexSize

import scala.collection.mutable.ListBuffer

object RedisReader {
  case class QueryLeaderboard(query: String, leaderboard: List[ExpertRating])
  case class ExpertRating(query: String, username: String, rating: Double)
  case class UserFeatureRequest(screenName: String)
  case class UserFeatures(
                         screenName: String,
                         tweetCount: Int,
                         followerCount: Int,
                         wordCount: Int,
// TODO                  punctuationCounts: Map[String, Int],  Temporarily disabled
                         capitalisedCount: Int,
                         hashtagCount: Int,
                         retweetCount: Int,
                         likeCount: Int,
                         dictionaryHits: Int,
                         linkCount: Int
                         )
  case class HasStatusBeenProcessed(status: twitter4j.Status)

  def extractFeatureCount(map: Map[String, String], featureName: String): Int = {
    map.get(featureName) match {
      case Some(count) => count.toInt
      case None => 0
    }
  }
}

@Singleton
class RedisReader @Inject()
(
  @Named("webSocketSupervisor") webSocketSupervisor: ActorRef
) extends Actor {

  import RedisReader._

  private val clients = RedisConnectionPool.pool

  override def receive = {

    case GetQueryMentionCounts(query) =>
      // i.e. ZRANGE hashtags:#query 0 20 WITHSCORES
      val cleanQuery = if (!query.startsWith("#")) s"#$query" else query
      val queryResult = clients.withClient{client =>
        client.zrangeWithScore(s"hashtags:$cleanQuery", 0, 20, DESC)
      }
      queryResult match {
        case Some(leaderboard) =>
          sender ! QueryLeaderboard(query, leaderboard.map {
            case (username, rating) => ExpertRating(query, username, rating)
          })
      }

    case GetRecentQueryList() =>
      val recentQueryResult = clients.withClient{client =>
        client.lrange("recentQueries", 0, 6)
      }
      var resultBatch = ListBuffer[String]()
      recentQueryResult match {
        case Some(queryResults) =>
          queryResults.foreach {
            case Some(r) =>
              resultBatch += r
          }
      }
      sender ! RecentQueries(resultBatch.toSet)

    case UserFeatureRequest(screenName) =>
      clients.withClient{client =>
        client.hgetall(s"user:$screenName:stats")
      } match {
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
            linkCount=extractFeatureCount(userFeatureMap, "linkCount")
          )
        case None => Logger.debug("Queried a non-existent user in Redis.")
      }

    case HasStatusBeenProcessed(status) =>
      clients.withClient{client =>
        val statusId = status.getId
        // Respond with a true or false
        sender ! (statusId, client.sismember(s"user:${status.getUser.getScreenName}:tweetIds", statusId))
      }

    case GetLatestIndexSize() =>
      clients.withClient{client =>
        client.get("indexSize") match {
          case Some(size) => sender ! LatestIndexSize(size.toInt)
          case None => Logger.error("Unable to determine the latest index size.")
        }
      }
  }

}
