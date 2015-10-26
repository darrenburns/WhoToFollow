package actors

import actors.UserHashtagCounter.HashtagCount
import akka.actor.Actor
import com.google.inject.{Inject, Singleton}


/*
 Reports can be sent here for storage in Redis.
 */
object RedisDispatcher {
  case class HashtagCountUpdate(results: List[HashtagCount])
}

@Singleton
class RedisDispatcher @Inject() extends Actor {

  import RedisDispatcher._

  override def receive = {
    case HashtagCountUpdate(results) =>
      applyHashtagCounts(results)
  }

  def applyHashtagCounts(hashtagCounts: Seq[HashtagCount]): Unit = {
    // TODO: Store results in Redis HashMap (adding to previous values).
  }

}

