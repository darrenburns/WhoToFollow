package modules

import actors.{TweetStreamActor, WebSocketSupervisor, PipelineSupervisor, WordCountActor}
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorModule extends AbstractModule with AkkaGuiceSupport {
  def configure() {
    bindActor[WordCountActor]("wordCountActor")
    bindActor[PipelineSupervisor]("pipelineSupervisor")
    bindActor[WebSocketSupervisor]("webSocketSupervisor")
    bindActor[TweetStreamActor]("tweetStreamActor")
  }
}
