package controllers

import actors.WebSocketSupervisor.OutputChannel
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.Inject
import com.google.inject.name.Named
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.duration._

class Application @Inject()
  (@Named("webSocketSupervisor") webSocketSupervisor: ActorRef) extends Controller {

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
}

