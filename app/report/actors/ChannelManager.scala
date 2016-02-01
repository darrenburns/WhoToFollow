package report.actors

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import com.google.inject.name.Named
import learn.actors.BatchFeatureExtraction.FetchAndAnalyseTimeline
import learn.actors.Indexer
import persist.actors.RedisReader.{UserFeatureRequest, UserFeatures}
import play.api.{Configuration, Logger}
import query.actors.QueryService.{Query, TerrierResultSet}
import report.actors.ChannelManager.UserTerrierScore
import report.actors.WebSocketSupervisor.QueryResults
import report.utility.ChannelUtilities

import scala.concurrent.duration._
import scala.util.{Failure, Success}



object ChannelManager {

  trait Factory {
    def apply(query: String): Actor
  }

  case class GetQueryMentionCounts(query: String)
  case class UserTerrierScore(screenName: String, name: String, score: Double)
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
   @Named("queryService") queryService: ActorRef,
   config: Configuration,
   @Assisted queryString: String)
   extends Actor {

  import context.dispatcher

  implicit val timeout = Timeout(20 seconds)

  val resultSetSize = config.getInt("resultSetSize").getOrElse(15)

  // Handle the query every 5 seconds
  val tick = context.system.scheduler.schedule(Duration.Zero, 5.seconds, self, Query(queryString))

  Logger.info(s"ChannelManager for channel '$queryString' created")

  override def receive = {
    case Query(query) =>
      if (ChannelUtilities.isQueryChannel(query)) {
        Logger.debug(s"Passing query '$query' to QueryService.")
        queryService ? Query(queryString) onComplete {
          case Success(results: TerrierResultSet) =>
            Logger.debug("ChannelManager has received results from QueryService.")

            val resultSet = results.resultSet
            val docIds = resultSet.getDocids
            val metaIndex = Indexer.index.getMetaIndex

            // Get a list of usernames and screennames from the docIds
            val profiles = docIds.map(docId => {
              // Get the username metadata for the current docId
              val usernameOption = Option(metaIndex.getItem("username", docId))
              val nameOption = Option(metaIndex.getItem("name", docId))
              (usernameOption, nameOption) match {
                case (Some(username), Some(name)) =>
                  batchFeatureExtraction ! FetchAndAnalyseTimeline(username)
                  (username, name)
                case (None, None) =>
                  Logger.error("USERNAME metadata not found in document.")
                  (docId.toString, docId.toString)
              }
            })


            // Get the sequence of user -> score
            val scores = resultSet.getScores
            val queryResults = (profiles zip scores) map {
              case ((screenName: String, name: String), score: Double) =>
                UserTerrierScore(screenName=screenName, name=name, score)
            }

            // Send the results through the socket for display on the UI
            webSocketSupervisor ! QueryResults(queryString, queryResults)

        }
//        redisReader ? GetQueryMentionCounts(query) onComplete {
//          case Success(ql: QueryLeaderboard) =>
//            // Get the timelines of all the users here and send them for analysis
//            val twitter = TwitterFactory.getSingleton
//            val analysisF = ql.leaderboard.map(r => Future {
//              val tweets = twitter.getUserTimeline(r.username)
//              if (tweets.nonEmpty) {
//                Logger.debug(s"Sending batch of tweets from timeline of ${r.username}: " + tweets.size())
//                batchFeatureExtraction ! TweetBatch(tweets.toList)
//                index ! TweetBatch(tweets.toList)
//              }
//            })
//            webSocketSupervisor ! ql
//          case Failure(error) => Logger.error("Error retrieving latest initial query ranks.", error)
//        }
      } else if (ChannelUtilities.isUserAnalysisChannel(query)) {
        ChannelUtilities.getScreenNameFromChannelName(query) match {
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
//    tick.cancel()  // Tell the scheduler to stop sending the scheduled message TODO readd when rdy
  }

}
