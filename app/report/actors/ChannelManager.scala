package report.actors

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import com.google.inject.name.Named
import persist.actors.RedisReader.{UserFeatures, UserFeatureRequest, QueryLeaderboard}
import play.api.Logger
import report.utility.ChannelUtilities

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}


object ChannelManager {

  trait Factory {
    def apply(query: String): Actor
  }

  case class GetQueryMentionCounts(query: String)
  case class DoTheRightThing(channelName: String)
}

/*
 Handles the fetching of data for the given channel.
 */
class ChannelManager @Inject()
  (@Named("redisReader") redisReader: ActorRef,
   @Named("redisWriter") redisWriter: ActorRef,
   @Named("webSocketSupervisor") webSocketSupervisor: ActorRef,
   @Assisted query: String)
   extends Actor {

  import ChannelManager._
  import context.dispatcher

  implicit val timeout = Timeout(20 seconds)

  // Handle the query every half second TODO CHANGE
  val tick = context.system.scheduler.schedule(Duration.Zero, 5.seconds, self, DoTheRightThing(query))

  Logger.info(s"ChannelManager for channel '$query' created")

  override def receive = {
    case DoTheRightThing(channelName) =>
      Logger.debug("Doing the.. probably wrong.... thing")
      if (ChannelUtilities.isQueryChannel(channelName)) {
        Logger.debug(s"Askin redis 4 the mention counts for the query $channelName")
        redisReader ? GetQueryMentionCounts(channelName) onComplete {
          case Success(ql: QueryLeaderboard) => webSocketSupervisor ! ql
          case Failure(error) => Logger.error("Error retrieving latest initial query ranks.", error)
        }
      } else if (ChannelUtilities.isUserAnalysisChannel(channelName)) {
        Logger.debug("Asking redis for user features")
        ChannelUtilities.getScreenNameFromChannelName(channelName) match {
          case Some(screenName) =>
            // Ask Redis for everything required and compose the futures
            redisReader ? UserFeatureRequest(screenName) onComplete {
              case Success(features: UserFeatures) =>
                Logger.debug("Redis sending features to wss")
                webSocketSupervisor ! features
              case Failure(error) => Logger.error("Error retrieving latest user features.", error)
            }
        }
      }
  }

  override def postStop() = {
    tick.cancel()  // Tell the scheduler to stop sending the scheduled message
  }

}
