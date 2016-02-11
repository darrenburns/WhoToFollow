package di

import learn.actors._
import persist.actors._
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
import query.actors.QueryService
import channels.actors._

class ActorModule extends AbstractModule with AkkaGuiceSupport {
  def configure() {
    /*
    1) No implementation for channels.actors.GenericChannel$Factory was bound.
  while locating channels.actors.GenericChannel$Factory
    for parameter 0 at channels.actors.UserChannelSupervisor.<init>(UserChannelSupervisor.scala:17)
  while locating channels.actors.UserChannelSupervisor
     */
    bindActor[UserHashtagCounter]("userHashtagCounter")
    bindActor[WebSocketSupervisor]("webSocketSupervisor")
    bindActor[TweetStreamActor]("tweetStreamActor")
    bindActor[UserChannelSupervisor](UserChannelSupervisor.name)
    bindActor[QuerySupervisor](QuerySupervisor.name)
    bindActor[RedisActor](RedisActor.name)
    bindActor[FeatureExtraction]("featureExtraction")
    bindActor[BatchFeatureExtraction](BatchFeatureExtraction.name)
    bindActor[Indexer](Indexer.name)
    bindActor[LabelStore]("labelStore")
    bindActor[QueryService](QueryService.name)
    bindActor[MetricsReporting](MetricsReporting.name)
    bindActor[UserMetadataWriter]("userMetadataWriter")
    bindActor[UserMetadataReader]("userMetadataReader")
    bindActorFactory[UserChannelWorker, UserChannelWorker.Factory]
    bindActorFactory[QueryWorker, QueryWorker.Factory]
  }
}
