package actors

import actors.RedisReader.QueryLeaderboard
import akka.actor.{ActorRef, Props, Actor}
import akka.util.Timeout
import play.api.Logger
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
   @Named("redisWriter") redisWriter: ActorRef,
   @Named("webSocketSupervisor") webSocketSupervisor: ActorRef,
   @Assisted query: String)
   extends Actor {

  import QueryHandler._
  import context.dispatcher

  implicit val timeout = Timeout(20 seconds)

  // Fetch the latest leaderboard for this query every 3 seconds
  val tick = context.system.scheduler.schedule(Duration.Zero, 3.seconds, self, FetchLatestQueryExperts(query))

  Logger.info(s"QueryHandler for channel '$query' created")

  override def receive = {
    // Will execute every `tick` seconds
    case req @ FetchLatestQueryExperts(q) =>
      val future = redisReader ? req
      val leaderboard = Await.result(future, timeout.duration).asInstanceOf[QueryLeaderboard]
      // Right now, we don't do any further processing, just send to client
      Logger.info(s"QueryHandler for channel '$q' sending results to WebSocketSupervisor")
      webSocketSupervisor ! leaderboard
  }

  override def postStop() = {
    tick.cancel()  // Tell the scheduler to stop sending the scheduled message
  }

}
