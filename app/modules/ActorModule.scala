package modules

import learn.actors._
import persist.actors.{RedisReader, RedisWriter, LabelStore}
import query.actors.QueryHandler
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
import report.actors.{MetricsReporting, WebSocketSupervisor}

class ActorModule extends AbstractModule with AkkaGuiceSupport {
  def configure() {
    bindActor[UserHashtagCounter]("userHashtagCounter")
    bindActor[WebSocketSupervisor]("webSocketSupervisor")
    bindActor[TweetStreamActor]("tweetStreamActor")
    bindActor[RedisWriter]("redisWriter")
    bindActor[RedisReader]("redisReader")
    bindActor[FeatureExtraction]("featureExtraction")
    bindActor[BatchFeatureExtraction]("batchFeatureExtraction")
    bindActor[Indexer]("indexer")
    bindActor[LabelStore]("labelStore")
    bindActor[MetricsReporting]("metricsReporting")
    bindActorFactory[QueryHandler, QueryHandler.Factory]
  }
}
