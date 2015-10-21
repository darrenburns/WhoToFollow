package controllers

import actors.ChannelTriple
import actors.WebSocketSupervisor.OutputChannel
import akka.actor.ActorRef
import akka.actor.Status.{Failure, Success}
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.Inject
import com.google.inject.name.Named
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future
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
    val future = (webSocketSupervisor ? OutputChannel(name)).mapTo[Either[Result, (Iteratee[JsValue, _], Enumerator[JsValue])]]
    future
    // TODO continue here
//    val f = future onSuccess {case ch =>
//      Future.successful(ch match {
//            case Success(chTriple: ChannelTriple) => Right((chTriple.in, chTriple.out))
//            case Failure(_) => Left(NotFound)
//        })
//    }

  }
}

