package actors

import actors.UserHashtagCounter.{UserHashtagReport, UserHashtagCount}
import akka.actor.Actor
import com.google.inject.{Inject, Singleton}
import init.RedisInit


/*
 Reports can be sent here for storage in Redis.
 */
object RedisWriter {
  case class HashtagCountUpdate(results: List[UserHashtagCount])
}

@Singleton
class RedisWriter @Inject() extends Actor {

  private val r = RedisInit.redis

  override def receive = {
    case UserHashtagReport(results) =>
      applyHashtagCounts(results)
  }

  def applyHashtagCounts(hashtagCounts: Seq[UserHashtagCount]): Unit = {
    hashtagCounts.foreach(userTagCount => {
      r.zincrby(s"hashtags:${userTagCount.hashtag}", userTagCount.count, userTagCount.username)
    })
  }

}

