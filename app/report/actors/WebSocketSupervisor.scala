package report.actors

import java.nio.channels.ClosedChannelException
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import com.github.nscala_time.time.Imports._
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.joda.time.DateTime
import persist.actors.{RedisReader, RedisWriter}
import RedisReader.{ExpertRating, QueryLeaderboard}
import persist.actors.RedisWriter
import RedisWriter.NewQuery
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import query.actors.QueryHandler
import report.actors.MetricsReporting.RecentQueries
import report.utility.KeepAlive

import scala.collection.immutable.HashMap
import scala.concurrent.duration.{Duration, FiniteDuration}

object WebSocketSupervisor {

  object Defaults {
    val RecentQueriesChannelName = "default:recent-queries"
    val LatestIndexSizeChannelName = "default:index-size"
    val DeadChannelTimeout = 30
  }

  implicit val expertRatingWrites = new Writes[ExpertRating] {
    def writes(rating: ExpertRating) = Json.obj(
      "query" -> rating.query,
      "username" -> rating.username,
      "rating" -> rating.rating
    )
  }

  implicit val recentQueriesWrites = new Writes[RecentQueries] {
    def writes(recentQueries: RecentQueries) = Json.obj(
      "recentQueries" -> recentQueries.recentQueriesList
    )
  }

  case class OutputChannel(name: String)
  case class CheckForDeadChannels()
  case class ChannelTriple(in: Iteratee[JsObject, Unit], out: Enumerator[JsValue],
                          channel: Concurrent.Channel[JsValue])
  case class LatestIndexSize(count: Int)
}




@Singleton
class WebSocketSupervisor @Inject()
(
  queryHandlerFactory: QueryHandler.Factory,
  @Named("redisWriter") redisWriter: ActorRef
) extends Actor with InjectedActorSupport {

  import WebSocketSupervisor._

  protected[this] var channels: HashMap[String, ChannelTriple] = HashMap.empty
  protected[this] var queryHandlers: HashMap[String, ActorRef] = HashMap.empty
  protected[this] var keepAlives: HashMap[String, DateTime] = HashMap.empty

  // Create the default:recent-queries channel which will handle sending recent queries to client
  createChannel(Defaults.RecentQueriesChannelName)
  createChannel(Defaults.LatestIndexSizeChannelName)

  implicit val timeout = Timeout(20, TimeUnit.SECONDS)


  context.system.scheduler.schedule(Duration.Zero, FiniteDuration(20, TimeUnit.SECONDS), self, CheckForDeadChannels())

  override def receive = {

    /*
     Request for an output channel fetch/creation depending on whether it is already
     active or not. Triggered when the client makes a new query and thus requests
     the creation of a new WebSocket.
     */
    case OutputChannel(query) =>
      channels.get(query) match {
        case Some(ch) =>
          Logger.info(s"Fetching existing channel: $query")
          sender ! Right((ch.in, ch.out))
        case None =>
          Logger.info(s"Creating channel for query $query.")
          redisWriter ! NewQuery(query)
          val ch = createChannel(query)
          sender ! (ch match {
            case chTriple: ChannelTriple => Right((chTriple.in, chTriple.out))
            case _ => Left("Not found")
          })
      }

    /*
     The expertise rankings retrieved for a given query. Results in the rankings/leaderboard
     being sent through the channel associated with that query.
     */
    case QueryLeaderboard(query, leaderboard) =>
      val json = Json.toJson(leaderboard)
      val channelTriple = channels.get(query)
      channelTriple match {
        case Some(triple) =>
          try {
            triple.channel push json
          } catch {
            case cce: ClosedChannelException =>
              Logger.info(s"A client closed the connection to channel $query.")
          }
        case None => Logger.error("Trying to send leaderboard through non-existent channel.")
      }

    /*
     Self-sent message. This actor messages itself repeatedly, requesting that any channels
     which haven't received a Keep-Alive within a set timeout range have their resources freed.
     */
    case CheckForDeadChannels() =>
      keepAlives.foreach(channel => {
        val channelName = channel._1
        val lastKeepAlive = channel._2
        if ((lastKeepAlive to DateTime.now).millis > Defaults.DeadChannelTimeout*1000) {
          // No Keep-Alives received within the specified time frame, so free resources
          Logger.info(s"Closing channel $channelName.")
          channels.get(channelName) match {
            case Some(chTriple) =>
              chTriple.channel.eofAndEnd()
              channels -= channelName
            case None => Logger.error("Trying to close a non-existent channel.")
          }
          queryHandlers.get(channelName) match {
            case Some(handler) =>
              context stop handler
              queryHandlers -= channelName
            case None => Logger.error(s"Tried to stop a non-existent QueryHandler for query $channelName")
          }
          keepAlives -= channelName
        }
      })

    /*
     The list of recent queries to display on the homepage of the UI
     */
    case msg @ RecentQueries(queries) =>
      val primaryChannelTriple = channels.get(Defaults.RecentQueriesChannelName)
      primaryChannelTriple match {
        case Some(chTriple) =>
          val json = Json.toJson(msg)
          chTriple.channel push json
        case None => Logger.error(s"Channel ${Defaults.RecentQueriesChannelName} does not exist.")
      }

    /*
    The most recent index size recorded in Redis.
     */
    case LatestIndexSize(size) =>
      Logger.debug("Received latest index size at socket output: " + size)
      val indexSizeChannelTriple = channels.get(Defaults.LatestIndexSizeChannelName)
      indexSizeChannelTriple match {
        case Some(chTriple) =>
          val json = Json.obj("indexSize" -> size)
          chTriple.channel push json
        case None => Logger.error(s"Channel ${Defaults.LatestIndexSizeChannelName} does not exist.")

      }

  }

  /**
    * Creates a new open channel for the given input query. The results for that query will be sent through
    * the open channel every time MetricsReporting asks for it.
    * @param query The name of the new channel (corresponds to the query text).
    * @return
    */
  private def createChannel(query: String) = {
    val (out, channel) = Concurrent.broadcast[JsValue]

    // Listen for Keep-Alives
    val in = Iteratee.foreach[JsObject] {q =>
      (q \ "request").as[String] match {
        case KeepAlive.messageType => sendKeepAlive((q \ "channel").as[String])
      }
    }

    // Construct and keep a reference to the channel components
    val chTriple = ChannelTriple(in, out, channel)
    channels += (query -> chTriple)

    // Ensure that the default channels (for metrics etc.) are not overridden
    if (query != Defaults.RecentQueriesChannelName && query != Defaults.LatestIndexSizeChannelName) {
      queryHandlers += (query -> injectedChild(queryHandlerFactory(query), query))
      keepAlives += (query -> DateTime.now)
    }
    chTriple
  }

  /**
    In order to keep track of which WebSockets are still needed,
   the client repeatedly sends keep-alive messages directed at
   the channel they are interested in. The client does this every
   js/util/config.keepAliveFrequency seconds. Also every Defaults.DeadChannelTimeout
   seconds, we check the most recent keep-alive time. If a keep-alive hasn't
   been sent in the past minute for a given channel, then all resources for
   that channel are closed
  */
  private def sendKeepAlive(channelName: String): Unit = {
    Logger.info(s"Received keep-alive directed to channel $channelName")
    keepAlives += (channelName -> DateTime.now())
  }

}
