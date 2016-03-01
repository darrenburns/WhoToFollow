package channels.actors

import akka.actor.{ActorRef, Actor}
import channels.actors.ChannelMessages.{CloseChannel, CreateChannel}
import com.google.inject.name.Named
import com.google.inject.{Singleton, Inject}
import di.NamedActor
import org.joda.time.DateTime
import persist.actors.RedisActor
import persist.actors.RedisWriterWorker.NewQuery
import play.api.Logger
import play.api.libs.concurrent.InjectedActorSupport
import query.actors.QueryService.TerrierResultSet

import scala.collection.immutable.HashMap


object QuerySupervisor extends NamedActor {
  final val name = "QuerySupervisor"

  var numQueries = 0
}

@Singleton
class QuerySupervisor @Inject() (
  @Named(MetricsReporting.name) metricsReporting: ActorRef,
  @Named(RedisActor.name) redisActor: ActorRef,
  workerFactory: QueryWorker.Factory
) extends Actor with InjectedActorSupport with GenericChannel.Supervisor {

  import QuerySupervisor._

  override var channels: HashMap[String, ChannelMeta] = HashMap.empty

  override def receive = {
    case CreateChannel(queryString) =>
      val query = NewQuery(queryString, numQueries, DateTime.now)
      metricsReporting ! query
      redisActor ! query
      numQueries += 1

      val channelMeta = getOrCreateChannel(queryString, workerFactory)
      sender ! (channelMeta match {
        case meta: ChannelMeta => Right((meta.in, meta.out))
        case _ => Left("Channel fetch/create error.")
      })
    case CloseChannel(channelName) => closeChannel(channelName)
    case resultSet @ TerrierResultSet(queryString, actualResultSize, results) =>
      channels.get(queryString) match {
        case Some(cMeta) => cMeta.actor forward resultSet
        case None => Logger.error("Received a ResultSet for an actor that no longer exists (it may have been " +
          "closed in the time since the last update request was received).")
      }
  }

}
