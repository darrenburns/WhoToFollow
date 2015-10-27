package actors

import actors.RedisReader.QueryLeaderboard
import akka.actor.{ActorRef, Props, Actor}
import akka.util.Timeout
import play.api.libs.concurrent.InjectedActorSupport
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import com.google.inject.name.Named
import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration._


object QueryHandler {

  trait Factory {
    def apply(query: String): Actor
  }

  case class FetchLatestQueryExperts(query: String)
}

/*
 Handles the fetching of data for the given query.
 */
class QueryHandler @Inject()
  (@Named("redisReader") redisReader: ActorRef,
   @Named("webSocketSupervisor") webSocketSupervisor: ActorRef,
   @Assisted query: String)
   extends Actor {

  import QueryHandler._
  import context.dispatcher

  implicit val timeout = Timeout(5 seconds)

  // Fetch the latest leaderboard for this query every 2 seconds
  val tick = context.system.scheduler.schedule(Duration.Zero, 500.millis, self, FetchLatestQueryExperts(query))

  println(s"QueryHandler for '$query' created")

  override def receive = {
    case req @ FetchLatestQueryExperts(q) =>
      val future = redisReader ? req
      val leaderboard = Await.result(future, timeout.duration).asInstanceOf[QueryLeaderboard]
      // Right now, we don't do any further processing, just send to client
      println(s"QueryHandler for '$q' sending results to WSS")
      webSocketSupervisor ! leaderboard
  }

}
