package channels.actors

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import channels.actors.MetricsReporting.GetMetricsChannel
import com.google.inject.{Singleton, Inject}
import com.google.inject.name.Named
import di.NamedActor
import learn.actors.HashtagCounter.HashtagCount
import learn.actors.Indexer
import learn.actors.Indexer.GetCollectionStats
import persist.actors.RedisActor
import persist.actors.RedisQueryWorker.GetRecentQueryList
import persist.actors.RedisWriterWorker.NewQuery
import play.api.Logger
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.json.{Writes, Json, JsObject, JsValue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, _}
import scala.util.{Failure, Success}


object MetricsReporting extends NamedActor {
  final val name = "MetricsReporting"
  case class RecentQueries(recentQueriesList: List[String])
  case class CollectionStats(numberOfDocuments: Int)
  case class NumberOfUsersSeen(numUsers: Int)
  case class TrendingHashtags(counts: List[HashtagCount])
  case object GetMetricsChannel

  implicit val newQueryWrites = new Writes[NewQuery] {
    def writes(nq: NewQuery) = Json.obj(
      "query" -> nq.query,
      "id" -> nq.id,
      "timestamp" -> nq.timestamp.getMillis
    )
  }

  implicit val collectionStatsWrites = new Writes[CollectionStats] {
    def writes(stats: CollectionStats) = Json.obj(
      "numDocs" -> stats.numberOfDocuments
    )
  }

  implicit val recentQueriesWrites = new Writes[RecentQueries] {
    def writes(recentQueries: RecentQueries) = Json.obj(
      "recentQueries" -> recentQueries.recentQueriesList
    )
  }

  implicit val numberOfUsersSeenWrites = new Writes[NumberOfUsersSeen] {
    def writes(numberOfUsersSeen: NumberOfUsersSeen) = Json.obj(
      "numUsersSeen" -> numberOfUsersSeen.numUsers
    )
  }

  implicit val hashtagCountWrites = new Writes[HashtagCount] {
    def writes(hashtagCount: HashtagCount) = Json.obj(
      "hashtag" -> hashtagCount.hashtag,
      "count" -> hashtagCount.count
    )
  }

  implicit val trendingHashtagsWrites = new Writes[TrendingHashtags] {
    def writes(trendingHashtags: TrendingHashtags) = Json.obj(
      "trendingHashtags" -> trendingHashtags.counts
    )
  }


}

@Singleton
class MetricsReporting @Inject() (
   @Named(RedisActor.name) redisActor: ActorRef,
   @Named(Indexer.name) indexer: ActorRef)
  extends Actor {

  import MetricsReporting._

  implicit val timeout = Timeout(20 seconds)

  val channelMeta = createMetricsChannel()

  context.system.scheduler.schedule(Duration.Zero, 2.seconds, redisActor, GetRecentQueryList)
  context.system.scheduler.schedule(Duration.Zero, 500.millis, indexer, GetCollectionStats)

  override def receive = {
    case GetMetricsChannel => sender ! (getMetricsChannelMeta match {
      case meta: ChannelMeta => Right((meta.in, meta.out))
      case _ => Left("Error fetching metrics channel.")
    })
    case recentQueries @ RecentQueries(_) => channelMeta.channel push Json.toJson(recentQueries)
    case stats @ CollectionStats(_) => channelMeta.channel push Json.toJson(stats)
    case query @ NewQuery(_,_,_) => channelMeta.channel push Json.toJson(query)
    case numUsers @ NumberOfUsersSeen(_) => channelMeta.channel push Json.toJson(numUsers)
    case trendingHashtags @ TrendingHashtags(_) =>
      Logger.debug("Pushing hashtag counts")
      channelMeta.channel push Json.toJson(trendingHashtags)
  }

  private def getMetricsChannelMeta = channelMeta

  private def createMetricsChannel(): ChannelMeta = {
    val (out, channel) = Concurrent.broadcast[JsValue]
    val in = Iteratee.ignore[JsObject]
    ChannelMeta(in, out, channel, self)
  }

}
