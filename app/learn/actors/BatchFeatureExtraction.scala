package learn.actors

import akka.actor.{PoisonPill, Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.Inject
import com.google.inject.name.Named
import learn.actors.TweetStreamActor.TweetBatch
import learn.utility.ExtractionUtils
import persist.actors.RedisReader.HasStatusBeenProcessed
import persist.actors.RedisWriter.{ProcessedTweets, TweetFeatureBatch}
import play.api.Logger
import twitter4j.Status

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


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
  @Named("redisReader") redisRead: ActorRef
) extends Actor {

  implicit val timeout = Timeout(10 seconds)

  override def receive = {
    case TweetBatch(tweets: Seq[Status]) =>

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

          // Build a sequence of futures of tuples
          val batchTweetFuture = newTweets.map(status => Future {
            (status, ExtractionUtils.getStatusFeatures(status),
              ExtractionUtils.getHashtagCounts(status))
          })

          // When all futures are complete
          val finishedIteration = Future.sequence(batchTweetFuture)
          finishedIteration onComplete {
            case Success(results) =>
              val (newlyProcessedTweets, featuresList, hashtagReports) = results.unzip3
              // Mark these tweets as processed
              redisWrite ! ProcessedTweets(newlyProcessedTweets)
              // Send the features of the tweets in this batch to Redis
              redisWrite ! TweetFeatureBatch(featuresList)
              // Send the user/hashtag updates to Redis
              hashtagReports.foreach(report => {
                redisWrite ! report
              })
            case Failure(error) =>
              Logger.error("Error during feature extraction. This occurred whilst extracting features from" +
                "the tweets in a user timeline.", error)
          }

          self ! PoisonPill

        case Failure(error) => Logger.error("Unable to determine whether tweets have been seen before.")


      }

  }

}
