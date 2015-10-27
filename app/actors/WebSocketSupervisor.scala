package actors

import actors.RedisReader.{ExpertRating, QueryLeaderboard}
import actors.UserHashtagCounter.UserHashtagReport
import akka.actor.{ActorRef, Actor}
import com.google.inject.{Inject, Singleton}
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json.{JsValue, Json, Writes}
import twitter4j.Status
import utils.StringUtilities

import scala.collection.immutable.HashMap


object WebSocketSupervisor {
  case class OutputChannel(name: String)

  implicit val expertRatingWrites = new Writes[ExpertRating] {
    def writes(rating: ExpertRating) = Json.obj(
      "query" -> rating.query,
      "username" -> rating.username,
      "rating" -> rating.rating
    )
  }

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
class WebSocketSupervisor @Inject()
  (queryHandlerFactory: QueryHandler.Factory)
  extends Actor with InjectedActorSupport {
  import WebSocketSupervisor._

  protected[this] var channels: HashMap[String, ChannelTriple] = HashMap.empty
  protected[this] var queryHandlers: HashMap[String, ActorRef] = HashMap.empty

  // Create the default:primary channel which just sends tweets to the client
  createChannel("default:primary")

  override def receive = {
    case TweetBatch(tweets) =>
      val primaryChannelTriple = channels.get("default:primary")
      primaryChannelTriple match {
        case Some(chTriple) =>
          val json = Json.toJson(tweets)
          chTriple.channel push json
        case None =>
          println("Channel 'default:primary' doesn't exist.")
      }
    case OutputChannel(query) =>
      println("Get or Else")
      channels.get(query) match {
        case Some(ch) =>
          println("Channel already exists.")
          sender ! Right((ch.in, ch.out))
        case None =>
          println("Create channel called")
          val ch = createChannel(query)
          sender ! (ch match {
            case chTriple: ChannelTriple => Right((chTriple.in, chTriple.out))
            case _ => Left("Not found")
          })
      }
      println("Channels has length " + channels.size)
    case UserHashtagReport(results) =>
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
    case QueryLeaderboard(query, leaderboard) =>
      val json = Json.toJson(leaderboard)
      val channelTriple = channels.get(query)
      channelTriple match {
        case Some(triple) =>
          triple.channel push json
        case None => println("Trying to send leaderboard through non-existent channel.")
      }
  }

  def createChannel(query: String) = {
    val (out, channel) = Concurrent.broadcast[JsValue]
    val in = Iteratee.ignore[JsValue]
    val chTriple = ChannelTriple(in, out, channel)
    channels += (query -> chTriple)
    if (query != "default:primary" && query != "test") {
      queryHandlers += (query -> injectedChild(queryHandlerFactory(query), query))
    }
    chTriple
  }

}
