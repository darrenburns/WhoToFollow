package actors

import actors.QueryHandler.FetchLatestQueryExperts
import akka.actor.Actor
import com.redis.RedisClient.DESC
import init.RedisInit

object RedisReader {



  case class QueryLeaderboard(query: String, leaderboard: List[ExpertRating])
  case class ExpertRating(query: String, username: String, rating: Double)
}

class RedisReader extends Actor {

  import RedisReader._

  private val r = RedisInit.redis

  override def receive = {
    case FetchLatestQueryExperts(query) =>
      // i.e. ZRANGE hashtags:#query 0 20 WITHSCORES
      val cleanQuery = if (!query.startsWith("#")) s"#$query" else query
      val queryResult = r.zrangeWithScore(s"hashtags:$cleanQuery", 0, 20, DESC)
      queryResult match {
        case Some(leaderboard) =>
          sender ! QueryLeaderboard(query, leaderboard.map {
            case (username, rating) => ExpertRating(query, username, rating)
          })
      }
  }

}
