package channels.actors

import akka.actor.{PoisonPill, Actor, ActorRef}
import channels.actors.ChannelMessages.CloseChannel
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import play.api.Logger
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json.{JsValue, JsObject}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.HashMap

case class ChannelMeta(in: Iteratee[JsObject, Unit],
                       out: Enumerator[JsValue],
                       channel: Concurrent.Channel[JsValue],
                       actor: ActorRef
                      )

case object CheckExpired

sealed trait ClientMessage
case object KeepAlive extends ClientMessage

object ChannelMessages {
  val DefaultChannelExpiry = 15000
  case class CreateChannel(channelName: String)
  case class CloseChannel(channelName: String)
}

object GenericChannel {

  trait Factory {
    def apply(channelName: String, wsChannel: Concurrent.Channel[JsValue], parent: ActorRef): Actor
  }

  trait Worker extends Actor {

    def channelName: String
    def channelExpiry: Int
    var latestKeepAlive: DateTime

    def hasExpired: Boolean = {
      Logger.debug(s"Checking expiry for Worker [${self.path}]: Expired == ${(latestKeepAlive to DateTime.now).millis > channelExpiry}.")
      (latestKeepAlive to DateTime.now()).millis > channelExpiry
    }

    def suicideAndCleanup(supervisor: ActorRef): Unit = {
      Logger.debug(s"Cleaning up Worker [${self.path}].")
      self ! PoisonPill
      supervisor ! CloseChannel(channelName)
    }

    def handleKeepAlive(): Unit = {
      Logger.debug(s"Setting keep-alive in actor ${self.path}: " + latestKeepAlive)
      latestKeepAlive = DateTime.now()
    }

    def pushLatest(): Unit

  }

  trait Supervisor extends Actor with InjectedActorSupport {

    var channels: HashMap[String, ChannelMeta]

    def createChannelActor(channelName: String,
                           wsChannel: Concurrent.Channel[JsValue],
                           workerFactory: GenericChannel.Factory): ActorRef = {
      injectedChild(workerFactory(channelName, wsChannel, self), channelName)
    }


    def constructChannelMeta(channelName: String, workerFactory: GenericChannel.Factory): ChannelMeta = {
      val (out, channel) = Concurrent.broadcast[JsValue]
      // Listen for Keep-Alives
      val in = Iteratee.foreach[JsObject] {q =>
        Logger.debug("Received JSON message from client: " + q)
        (q \ "request").as[String] match {
          case "KEEP-ALIVE" =>
            val channelName = (q \ "channel").as[String]
            Logger.debug(s"Matched KEEP-ALIVE, channel is '$channelName'")
            channels.get((q \ "channel").as[String]) match {
              case Some(requestedChannelMeta) =>
                Logger.debug(s"Sending KeepAlive to ${requestedChannelMeta.actor.path}")
                requestedChannelMeta.actor ! KeepAlive
              case _ => Logger.error(s"Received a KeepAlive for a channel that doesn't exist anymore ($channelName).")
            }
        }
      }
      ChannelMeta(in, out, channel, createChannelActor(channelName, channel, workerFactory))
    }

    def getOrCreateChannel(channelName: String, workerFactory: GenericChannel.Factory): ChannelMeta = {
      channels.get(channelName: String) match {
        case Some(channelMeta) =>
          Logger.debug(s"Fetching existing channel '$channelName'.")
          channelMeta
        case _ =>
          Logger.debug(s"Opening new channel '$channelName'.")
          val meta = constructChannelMeta(channelName, workerFactory)
          channels = channels + (channelName -> meta)
          meta
      }
    }

    def closeChannel(channelName: String): Unit = {
      channels.get(channelName) match {
        case Some(channel) =>
          channel.channel.eofAndEnd()
          channels = channels - channelName
        case None =>
          Logger.error("Attempted to close a non-existent channel.")
      }
    }

  }

}



