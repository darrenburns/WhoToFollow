package report.actors

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import com.google.inject.name.Named
import learn.actors.TweetStreamActor.TweetBatch
import persist.actors.RedisReader.{UserFeatures, UserFeatureRequest, QueryLeaderboard}
import play.api.Logger
import query.actors.QueryService.Query
import report.utility.ChannelUtilities
import twitter4j.TwitterFactory

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.collection.JavaConversions._



object ChannelManager {

  trait Factory {
    def apply(query: String): Actor
  }

  case class GetQueryMentionCounts(query: String)
}

/*
 Handles the fetching of data for the given channel.
 */
class ChannelManager @Inject()
  (@Named("redisReader") redisReader: ActorRef,
   @Named("redisWriter") redisWriter: ActorRef,
   @Named("webSocketSupervisor") webSocketSupervisor: ActorRef,
   @Named("batchFeatureExtraction") batchFeatureExtraction: ActorRef,
   @Named("indexer") index: ActorRef,
   @Assisted query: String)
   extends Actor {

  import ChannelManager._
  import context.dispatcher

  implicit val timeout = Timeout(20 seconds)

  // Handle the query every half second TODO CHANGE
  val tick = context.system.scheduler.schedule(Duration.Zero, 5.seconds, self, DoTheRightThing(query))

  Logger.info(s"ChannelManager for channel '$query' created")

  override def receive = {
    case Query(query) =>
      if (ChannelUtilities.isQueryChannel(query)) {
        // TODO: Pass the query through Terrier here.
        redisReader ? GetQueryMentionCounts(query) onComplete {
          case Success(ql: QueryLeaderboard) =>
            // Get the timelines of all the users here and send them for analysis
            val twitter = TwitterFactory.getSingleton
            val analysisF = ql.leaderboard.map(r => Future {
              val tweets = twitter.getUserTimeline(r.username)
              if (tweets.nonEmpty) {
                Logger.debug(s"Sending batch of tweets from timeline of ${r.username}: " + tweets.size())
                batchFeatureExtraction ! TweetBatch(tweets.toList)
                index ! TweetBatch(tweets.toList)
              }
            })
            webSocketSupervisor ! ql
          case Failure(error) => Logger.error("Error retrieving latest initial query ranks.", error)
        }
      } else if (ChannelUtilities.isUserAnalysisChannel(channelName)) {
        ChannelUtilities.getScreenNameFromChannelName(channelName) match {
          case Some(screenName) =>
            // Ask Redis for everything required and compose the futures
            redisReader ? UserFeatureRequest(screenName) onComplete {
              case Success(features: UserFeatures) =>
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
