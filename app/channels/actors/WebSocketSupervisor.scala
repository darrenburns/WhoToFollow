package channels.actors

import java.nio.channels.ClosedChannelException
import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import channels.actors.MetricsReporting.CollectionStats
import persist.actors.RedisWriterWorker.NewQuery
import play.api.Logger
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json.{JsObject, JsValue, Json, Writes}

import scala.collection.immutable.HashMap

object WebSocketSupervisor {

  object Defaults {
    val RecentQueriesChannelName = "default:recent-queries"
    val LatestIndexSizeChannelName = "default:index-size"
    val DeadChannelTimeout = 30
  }


  implicit val newQueryWrites = new Writes[NewQuery] {
    def writes(nq: NewQuery) = Json.obj(
      "query" -> nq.query,
      "id" -> nq.id,
      "timestamp" -> nq.timestamp.getMillis
    )
  }

  case class OutputChannel(name: String)
  case class CheckForDeadChannels()
  case class ChannelTriple(in: Iteratee[JsObject, Unit], out: Enumerator[JsValue],
                          channel: Concurrent.Channel[JsValue])
}

class WebSocketSupervisor extends Actor with InjectedActorSupport {

  import WebSocketSupervisor._

  protected[this] var channels: HashMap[String, ChannelTriple] = HashMap.empty

  implicit val timeout = Timeout(20, TimeUnit.SECONDS)

  override def receive = {

    /*
    The most recent index size recorded in Redis.
     */
    case CollectionStats(numDocs) =>
      val indexSizeChannelTriple = channels.get(Defaults.LatestIndexSizeChannelName)
      indexSizeChannelTriple match {
        case Some(chTriple) =>
          val json = Json.obj("indexSize" -> numDocs)
          try {
            chTriple.channel push json
          } catch {
            case cce: ClosedChannelException =>
              Logger.error("The latest index size channel is closed.")
            case e: Exception =>
              Logger.error("An exception occurred while trying to push the most recent index size.", e)
        }
        case None => Logger.error(s"Channel ${Defaults.LatestIndexSizeChannelName} does not exist.")

      }

  }



}
