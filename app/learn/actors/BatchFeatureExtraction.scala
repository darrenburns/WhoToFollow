package learn.actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.github.nscala_time.time.Imports._
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import di.NamedActor
import hooks.Twitter
import learn.actors.TweetStreamActor.TweetBatch
import learn.utility.ExtractionUtils
import org.joda.time.DateTime
import persist.actors.RedisActor
import persist.actors.RedisQueryWorker.HasStatusBeenProcessed
import persist.actors.RedisWriterWorker.{ProcessedTweets, TweetFeatureBatch}
import persist.actors.UserMetadataWriter.TwitterUser
import play.api.Logger
import twitter4j.{Paging, Status}

import scala.collection.JavaConversions._
import scala.collection.immutable.HashMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


object BatchFeatureExtraction extends NamedActor {
  override final val name = "BatchFeatureExtraction"
  case class FetchAndAnalyseTimeline(screenName: String)
  case class SetSimulationTime(newTime: DateTime)
  case object GetCurrentMaxStatusId
  case class SetCurrentMaxStatusId(maxId: Long)
}

/**
  * Handles feature extraction from status/textual data where a Spark streaming context
  * is not being used. The lack of a streaming context means that we can have a non-singleton
  * actor and therefore high levels of concurrency without delegating to Spark.
  *
  * This actor is primarily used when we are performing feature extraction on a user's
  * timeline, since they will generally be small and so don't warrant distributing across
  * Spark workers with SparkContext's parallelize() method.
  */
@Singleton
class BatchFeatureExtraction @Inject()
(
  @Named(RedisActor.name) redisActor: ActorRef,
  @Named(Indexer.name) indexer: ActorRef,
  @Named("userMetadataWriter") userMetadataWriter: ActorRef
) extends Actor {

  import BatchFeatureExtraction._

  implicit val timeout = Timeout(10, TimeUnit.SECONDS)

  // Map of screenName -> (StatusId, Last time their timeline was checked)
  protected[this] var latestUserChecks: HashMap[String, (Long, DateTime)] = HashMap.empty
  var simulationTime: Option[DateTime] = Some(new DateTime(0, 1, 1, 0, 0, 0, DateTimeZone.UTC))
  var currentMaxStatusId = 1L

  override def receive = {
    case FetchAndAnalyseTimeline(screenName: String) =>
      // Check to see when we last looked at this users timeline
      latestUserChecks.get(screenName) match {
        case Some((statusId, lastChecked)) =>
          // If we haven't looked at timeline in at least 6 hours then check again
          simulationTime match {
            case Some(simTime) =>
              if (lastChecked < simTime - 6.hours) {
                Logger.debug(s"Reanalysing user: $screenName")
                analyseUserTimeline(screenName, statusId)
              }
            case None =>
              Logger.error("Cannot determine current simulation time.")
          }
        case None =>
          // We have never seen this user before so we want to analyse their timeline
          Logger.debug(s"Analysing new user: $screenName")
          analyseUserTimeline(screenName, currentMaxStatusId)
      }

    case GetCurrentMaxStatusId => sender ! currentMaxStatusId
    case SetCurrentMaxStatusId(maxId) => setCurrentMaxStatusId(maxId)

    case TweetBatch(tweets: List[Status]) =>

      Logger.debug("Received new tweet batch from a user timeline")

      // Filter the list so that it only contains tweets we haven't seen before
      // Futures contain Tuple of (tweetId, haveWeSeenThisTweetBefore?)
      var tweetAnalysisHistory = scala.collection.immutable.Set.empty[Future[Any]]
      tweets.foreach(tweet => {
        tweetAnalysisHistory += redisActor ? HasStatusBeenProcessed(tweet)
      })

      // When we have results for all of the tweets
      Future.sequence(tweetAnalysisHistory) onComplete {
        case Success(seenBefore) =>

          // Keep only tweets we haven't seen before
          val newTweets = tweets.filter(tweet => !(seenBefore contains (tweet.getId, true)))

          // Build a sequence of futures of tuples
          val batchTweetFuture = newTweets.map(status => {
            // Perform feature extraction
            Future {
            (status, ExtractionUtils.getStatusFeatures(status),
              ExtractionUtils.getHashtagCounts(status))
            }
          })

          // When all futures are complete
          val finishedIteration = Future.sequence(batchTweetFuture)
          finishedIteration onComplete {
            case Success(results) =>
              val (newlyProcessedTweets, featuresList, hashtagReports) = results.unzip3
              Logger.debug(s"Finished analysis of ${newlyProcessedTweets.size}.")
              // Index the tweets we haven't seen before
              indexer ! TweetBatch(newTweets)
              // Mark these tweets as processed
              redisActor ! ProcessedTweets(newlyProcessedTweets)
              // Send the features of the tweets in this batch to Redis
              Logger.debug(s"Sending feature list of size ${featuresList.size} to Redis.")
              redisActor ! TweetFeatureBatch(featuresList)
              // Send the user/hashtag updates to Redis
              hashtagReports.foreach(report => {
                redisActor ! report
              })
            case Failure(error) =>
              Logger.error("Error during feature extraction. This occurred whilst extracting features from" +
                "the tweets in a user timeline.", error)
          }

        case Failure(error) => Logger.error("Unable to determine whether tweets have been seen before.")
      }
  }

  def setCurrentMaxStatusId(maxId: Long): Unit = currentMaxStatusId = maxId

  def analyseUserTimeline(screenName: String, maxId: Long): Unit = {
    simulationTime match {
      case Some(time) => markUserLastSeen(screenName, maxId, time)
      case None => Logger.error("Unable to determine current simulation time.")
    }
    // Cache the user metadata
    userMetadataWriter ! TwitterUser(Twitter.instance.showUser(screenName))
    // Analyse the tweets from the user timeline
    val paging = new Paging()
    Logger.debug(s"[CAPTURE] Paging on maxStatusId: $currentMaxStatusId")
    paging.setMaxId(maxId)
    paging.setCount(20)
    self ! TweetBatch(Twitter.instance.getUserTimeline(screenName, paging).toList)
  }

  def markUserLastSeen(screenName: String, lastStatusId: Long, lastSeen: DateTime): Unit = {
    latestUserChecks += (screenName -> (lastStatusId, lastSeen))
    simulationTime = Option(lastSeen)
  }
}
