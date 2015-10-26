package actors

import actors.UserHashtagCounter.{UserHashtagReport, UserHashtagCount, HashtagCount}
import akka.actor.Actor
import com.google.inject.{Inject, Singleton}
import init.RedisInit


/*
 Reports can be sent here for storage in Redis.
 */
object RedisDispatcher {
  case class HashtagCountUpdate(results: List[UserHashtagCount])
}

@Singleton
class RedisDispatcher @Inject() extends Actor {

  import RedisDispatcher._

  private val r = RedisInit.redis

  override def receive = {
    case UserHashtagReport(results) =>
      applyHashtagCounts(results)
  }

  def applyHashtagCounts(hashtagCounts: Seq[UserHashtagCount]): Unit = {
    // TODO: Store results in Redis HashMap (adding to previous values).
    hashtagCounts.foreach(userTagCount => {
      r.zincrby(s"hashtags:${userTagCount.hashtag}", userTagCount.count, userTagCount.username)
    })
  }

}

