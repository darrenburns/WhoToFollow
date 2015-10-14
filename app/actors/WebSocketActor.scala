//package actors
//
//import actors.UserActor.ResultUpdate
//import akka.actor.{Actor, Props, ActorRef}
//import play.api.Logger
//import play.api.libs.json.JsArray
//
//object WebSocketActor {
//  val log = Logger(getClass)
//  def props(out: ActorRef) = Props(classOf[WebSocketActor], out)
//}
//
//class WebSocketActor(out: ActorRef) extends Actor {
//  println("Creating WebSocket actor")
//  WordCountActor.wordCountActor ! self
//  override def receive = {
//    case UserActor.ResultUpdate(results) =>
//      println("WebSocketActor sending results: " + results)
//      println("to Actor: " + out)
//      out ! results
//  }
//
//}
