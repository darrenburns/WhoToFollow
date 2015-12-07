package controllers

import actors.LabelStore
import actors.LabelStore.Vote
import actors.WebSocketSupervisor.OutputChannel
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.Inject
import com.google.inject.name.Named
import play.api.Logger
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.{JsSuccess, JsResult, JsValue, Json}
import play.api.mvc._

import scala.concurrent.duration._


class Application @Inject()
(
  @Named("webSocketSupervisor") webSocketSupervisor: ActorRef,
  @Named("labelStore") labelStore: ActorRef
) extends Controller {

  implicit val timeout = Timeout(5 seconds)

  def index = Action {
    Ok(views.html.index())
  }

  def getChannel(name: String) = WebSocket.tryAccept[JsValue] { request =>
    // Ask the WebSocketSupervisor for the requested channel.
    // It will create it if it doesn't already exist.
    val future = (webSocketSupervisor ? OutputChannel(name))
                    .mapTo[Either[Result, (Iteratee[JsValue, _], Enumerator[JsValue])]]
    future
  }

  /**
    * Receives a POST request containing a Vote object which
    * is sent to the store for future machine learning tasks.
    * @return A HTTP 200 response
    */
  def voteForUser = Action { request =>
    val json = request.body.asJson.get
    val vote = json.as[Vote]
    Logger.debug(s"Action: Received vote for ${vote.screenName} in topic '${vote.hashtag}': Vote value = ${vote.voteId}")
    labelStore ! vote
    Ok // TODO: Assuming success
  }
}

