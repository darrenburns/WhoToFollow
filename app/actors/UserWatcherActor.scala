package actors

import akka.actor.{ActorRef, Props, Actor}
import akka.actor.Actor.Receive
import com.google.inject.Singleton
import play.api.libs.json.Json

import scala.collection.immutable.HashSet


case class AddAsWatcher()

object UserWatcherActor {
  def props() = Props(classOf[UserWatcherActor])
  // The set of UserActors watching this stream
  var watchers: HashSet[ActorRef] = HashSet.empty[ActorRef]
}

@Singleton
class UserWatcherActor extends Actor {
  import UserWatcherActor._

  override def receive = {
    case addAsWatcher @ AddAsWatcher() =>
      println("Received AddAsWatcher, Adding as watcher")
      watchers = watchers + sender
      println("Watchers has size " + watchers.size)
    case ResultUpdate(message) =>
      println("Received ResultUpdate, sending to watchers of size " + watchers.size)
      watchers.foreach(_ ! ResultUpdate(message))
  }

}

