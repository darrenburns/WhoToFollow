package modules

import learn.actors._
import persist.actors._
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
import query.actors.QueryService
import report.actors.{ChannelManager, MetricsReporting, WebSocketSupervisor}

class ActorModule extends AbstractModule with AkkaGuiceSupport {
  def configure() {
    bindActor[UserHashtagCounter]("userHashtagCounter")
    bindActor[WebSocketSupervisor]("webSocketSupervisor")
    bindActor[TweetStreamActor]("tweetStreamActor")

    bindActor[RedisActor]("redisActor")

    bindActor[FeatureExtraction]("featureExtraction")
    bindActor[BatchFeatureExtraction]("batchFeatureExtraction")
    bindActor[Indexer]("indexer")
    bindActor[LabelStore]("labelStore")
    bindActor[QueryService]("queryService")
    bindActor[MetricsReporting]("metricsReporting")
    bindActor[UserMetadataWriter]("userMetadataWriter")
    bindActor[UserMetadataReader]("userMetadataReader")
    bindActorFactory[ChannelManager, ChannelManager.Factory]
  }
}
