package modules

import actors.{UserWatcherActor, WordCountActor}
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorModule extends AbstractModule with AkkaGuiceSupport {
  def configure() {
    bindActor[WordCountActor]("wordCountActor")
    bindActor[UserWatcherActor]("userWatcherActor")
  }
}
