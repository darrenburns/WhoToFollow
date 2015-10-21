package actors

import actors.PipelineSupervisor.WordCountUpdate
import akka.actor.Actor
import akka.actor.Status.{Failure, Success}
import akka.util.Timeout
import com.google.inject.Singleton
import play.api.libs.iteratee.{Iteratee, Enumerator, Concurrent}
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.WebSocket

import scala.collection.immutable.HashMap


object WebSocketSupervisor {
  case class OutputChannel(name: String)
}

case class ChannelTriple(in: Iteratee[JsValue, Unit], out: Enumerator[JsValue],
                        channel: Concurrent.Channel[JsValue])

@Singleton
class WebSocketSupervisor extends Actor {
  import WebSocketSupervisor._

  protected[this] var channels: HashMap[String, ChannelTriple] = HashMap.empty

  override def receive = {
    case OutputChannel(name) =>
      println("Get or Else")
      channels.get(name) match {
        case Some(ch) =>
          println("Channel already exists.")
          sender ! ch
        case None =>
          println("Create channel called")
          val ch = createChannel(name)
          channels = channels + (name -> ch)
          sender ! (ch match {
            case chTriple: ChannelTriple => Right((chTriple.in, chTriple.out))
            case _ => Left("Not found")
          })
          /*
          Todo: A new QueryActor should be made, which
          retrieves all the results for the given query.
          The QueryActor should be given the new channel
          as a prop.
           */
      }
      println("Channels has length " + channels.size)
    case WordCountUpdate(results) =>
      // Convert the results to JSON and push
      // it through the broadcast channel
      val json = Json.toJson(results)
      println(json)
      // In the case of a word count update,
      // forward the result through the 'test'
      // channel in the HashMap
      val channelTriple = channels.get("test")
      channelTriple match {
        case Some(triple) =>
          triple.channel push json
        case None => println("Channel doesn't exist.")
      }


  }

  def createChannel(name: String) = {
    val (out, channel) = Concurrent.broadcast[JsValue]
    val in = Iteratee.ignore[JsValue]
    ChannelTriple(in, out, channel)
  }

}
