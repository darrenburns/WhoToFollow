package actors

import actors.MetricsReporting.{RecentQueries, GetRecentQueryList}
import actors.QueryHandler.FetchLatestQueryExperts
import akka.actor.Actor
import com.redis.RedisClient.DESC
import init.RedisConnectionPool

import scala.collection.mutable.ListBuffer

object RedisReader {



  case class QueryLeaderboard(query: String, leaderboard: List[ExpertRating])
  case class ExpertRating(query: String, username: String, rating: Double)
}

class RedisReader extends Actor {

  import RedisReader._

  private val clients = RedisConnectionPool.pool

  override def receive = {
    case FetchLatestQueryExperts(query) =>
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
  }

}
