package actors

import actors.UserHashtagCounter.{UserHashtagReport, UserHashtagCount}
import akka.actor.Actor
import com.google.inject.{Inject, Singleton}
import init.RedisConnectionPool


/*
 Reports can be sent here for storage in Redis.
 */
object RedisWriter {
  case class HashtagCountUpdate(results: List[UserHashtagCount])
}

@Singleton
class RedisWriter @Inject() extends Actor {

  private val clients = RedisConnectionPool.pool

  override def receive = {
    case UserHashtagReport(results) =>
      applyHashtagCounts(results)
  }

  def applyHashtagCounts(hashtagCounts: Seq[UserHashtagCount]): Unit = {
    clients.withClient{client =>
      hashtagCounts.foreach(userTagCount => {
        client.zincrby(s"hashtags:${userTagCount.hashtag}", userTagCount.count, userTagCount.username)
      })
    }
  }

}

