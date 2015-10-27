package modules

import actors._
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorModule extends AbstractModule with AkkaGuiceSupport {
  def configure() {
    bindActor[UserHashtagCounter]("userHashtagCounter")
    bindActor[WebSocketSupervisor]("webSocketSupervisor")
    bindActor[TweetStreamActor]("tweetStreamActor")
    bindActor[RedisWriter]("redisWriter")
    bindActor[RedisReader]("redisReader")
    bindActorFactory[QueryHandler, QueryHandler.Factory]
  }
}
