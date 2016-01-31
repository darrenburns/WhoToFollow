package learn.actors

import java.util.concurrent.TimeUnit

import com.github.nscala_time.time.Imports._
import akka.actor.{PoisonPill, Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.Inject
import com.google.inject.name.Named
import hooks.Twitter
import learn.actors.TweetStreamActor.TweetBatch
import learn.utility.ExtractionUtils
import org.joda.time.DateTime
import persist.actors.RedisReader.HasStatusBeenProcessed
import persist.actors.RedisWriter.{ProcessedTweets, TweetFeatureBatch}
import persist.actors.UserMetadataWriter.TwitterUser
import play.api.Logger
import twitter4j.{TwitterFactory, Status}

import scala.collection.immutable.HashMap
import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global


object BatchFeatureExtraction {
  case class FetchAndAnalyseTimeline(screenName: String)
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
class BatchFeatureExtraction @Inject()
(
  @Named("redisWriter") redisWrite: ActorRef,
  @Named("redisReader") redisRead: ActorRef,
  @Named("indexer") indexer: ActorRef,
  @Named("userMetadataWriter") userMetadataWriter: ActorRef
) extends Actor {

  import BatchFeatureExtraction._

  implicit val timeout = Timeout(10, TimeUnit.SECONDS)

  // Map of screenName -> Last time their timeline was checked
  protected[this] var latestUserChecks: HashMap[String, DateTime] = HashMap.empty

  override def receive = {
    case FetchAndAnalyseTimeline(screenName: String) =>
      // Check to see when we last looked at this users timeline
      latestUserChecks.get(screenName) match {
        case Some(lastChecked) =>
          // If we haven't looked at timeline in at least 6 hours then check again
          if (lastChecked < DateTime.now - 6.hours){
            analyseUserTimeline(screenName)
          }
        case None =>
          // We have never seen this user before so we want to analyse their timeline
          analyseUserTimeline(screenName)
      }

    case TweetBatch(tweets: List[Status]) =>

      Logger.debug("Received new tweet batch from a user timeline.")

      // Filter the list so that it only contains tweets we haven't seen before
      // Futures contain Tuple of (tweetId, haveWeSeenThisTweetBefore?)
      var tweetAnalysisHistory = scala.collection.immutable.Set.empty[Future[Any]]
      tweets.foreach(tweet => {
        tweetAnalysisHistory += redisRead ? HasStatusBeenProcessed(tweet)
      })

      // When we have results for all of the tweets
      Future.sequence(tweetAnalysisHistory) onComplete {
        case Success(seenBefore) =>
          // Keep only tweets we haven't seen before
          val newTweets = tweets.filter(tweet => !(seenBefore contains (tweet.getId, true)))

          Logger.debug(s"Batch analysing ${newTweets.size} tweets.")
          // Build a sequence of futures of tuples
          val batchTweetFuture = newTweets.map(status => {
            // Mark the time that we last reviewed this user
            latestUserChecks += (status.getUser.getScreenName -> DateTime.now)
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
              redisWrite ! ProcessedTweets(newlyProcessedTweets)
              // Send the features of the tweets in this batch to Redis
              Logger.debug(s"Sending feature list of size ${featuresList.size} to Redis.")
              redisWrite ! TweetFeatureBatch(featuresList)
              // Send the user/hashtag updates to Redis
              hashtagReports.foreach(report => {
                redisWrite ! report
              })
            case Failure(error) =>
              Logger.error("Error during feature extraction. This occurred whilst extracting features from" +
                "the tweets in a user timeline.", error)
          }

        case Failure(error) => Logger.error("Unable to determine whether tweets have been seen before.")
      }
  }

  def analyseUserTimeline(screenName: String): Unit = {
    // Cache the user metadata
    userMetadataWriter ! TwitterUser(Twitter.instance.showUser(screenName))
    // Analyse the tweets from the user timeline
    self ! TweetBatch(Twitter.instance.getUserTimeline(screenName).toList)
  }

}
