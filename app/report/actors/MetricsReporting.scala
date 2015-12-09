package report.actors

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.{Singleton, Inject}
import com.google.inject.name.Named
import play.api.Logger
import report.actors.MetricsReporting.{GetLatestIndexSize, GetRecentQueryList}
import report.actors.WebSocketSupervisor.LatestIndexSize

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, _}
import scala.util.{Failure, Success}


object MetricsReporting {
  case class RecentQueries(recentQueriesList: Set[String])
  case class GetRecentQueryList()
  case class GetLatestIndexSize()
}

@Singleton
class MetricsReporting @Inject()
  (@Named("redisReader") redisReader: ActorRef,
   @Named("webSocketSupervisor") webSocketSupervisor: ActorRef)
  extends Actor {

  implicit val timeout = Timeout(20 seconds)

  // Fetch the latest 'recent queries' list 1 second
  context.system.scheduler.schedule(Duration.Zero, 1.seconds, self, GetRecentQueryList())

  // Ask Redis for the latest index size every 1 second.
  context.system.scheduler.schedule(Duration.Zero, 1.seconds, redisReader, GetLatestIndexSize())

  override def receive = {
    /*
    Send the recent query list to all of the actors who are interested in it.
     */
    case msg @ GetRecentQueryList() =>
      (redisReader ? msg) onComplete {
        case Success(recentQueries) => webSocketSupervisor ! recentQueries
        case Failure(error) => Logger.debug("Error: " + error)
      }

    /*
    We've got the latest index size so send it to all the places who are interested in it.
     */
    case LatestIndexSize(size) =>
      webSocketSupervisor ! LatestIndexSize(size)
  }

}
