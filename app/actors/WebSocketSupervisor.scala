package actors

import actors.PipelineSupervisor.HashtagCountUpdate
import akka.actor.Actor
import com.google.inject.Singleton
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json.{JsValue, Json, Writes}
import twitter4j.Status

import scala.collection.immutable.HashMap


object WebSocketSupervisor {
  case class OutputChannel(name: String)


  implicit val statusWrites = new Writes[Status] {
    def writes(status: Status) = Json.obj(
      "id" -> status.getId,
      "text" -> status.getText,
      "username" -> status.getUser.getName,
      "screenname" -> status.getUser.getScreenName,
      "date" -> status.getCreatedAt,
      "retweets" -> status.getRetweetCount,
      "avatar" -> status.getUser.getProfileImageURL
    )
  }
}

case class ChannelTriple(in: Iteratee[JsValue, Unit], out: Enumerator[JsValue],
                        channel: Concurrent.Channel[JsValue])

@Singleton
class WebSocketSupervisor extends Actor {
  import WebSocketSupervisor._

  protected[this] var channels: HashMap[String, ChannelTriple] = HashMap.empty

  // Create the default:primary channel which just sends tweets to the client
  channels = channels + ("default:primary" -> createChannel("default:primary"))

  override def receive = {
    case TweetBatch(tweets) =>
      val primaryChannelTriple = channels.get("default:primary")
      primaryChannelTriple match {
        case Some(chTriple) =>
          val json = Json.toJson(tweets)
          println("Sending tweets to client")
          chTriple.channel push json
        case None =>
          println("Channel 'default:primary' doesn't exist.")
      }
    case OutputChannel(name) =>
      println("Get or Else")
      channels.get(name) match {
        case Some(ch) =>
          println("Channel already exists.")
          sender ! Right((ch.in, ch.out))
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
    case HashtagCountUpdate(results) =>
      // Convert the results to JSON and push
      // it through the broadcast channel
      val json = Json.toJson(results)
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
