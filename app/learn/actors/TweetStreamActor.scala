package learn.actors

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import hooks.SparkInit
import learn.actors.TweetStreamActor.{TweetBatch, Ready}
import learn.actors.UserHashtagCounter.ActiveTwitterStream
import org.apache.spark.streaming.twitter.TwitterUtils
import play.api.Logger
import twitter4j.Status
import utils.TwitterAuth

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


object TweetStreamActor {
  val DefaultBatchSize = 20
  case class TweetBatch(tweets: List[Status])
  case class Ready()
}

@Singleton
class TweetStreamActor @Inject()
(
   @Named("userHashtagCounter") userHashtagCounter: ActorRef,
   @Named("featureExtraction") featureExtraction: ActorRef,
   @Named("indexer") indexer: ActorRef
)
  extends Actor with TwitterAuth {

  implicit val timeout = Timeout(20 seconds)

  Logger.info("Initialising TweetStreamActor")
  checkTwitterKeys()
  val streamHandle = TwitterUtils.createStream(SparkInit.ssc, None)

  // Asynchronously send status stream handle to interested actors
  val readyFuture = for {
    f1 <- userHashtagCounter ? ActiveTwitterStream(streamHandle)
    f2 <- featureExtraction ? ActiveTwitterStream(streamHandle)
  } yield (f1, f2)

  Logger.info("Stream handle sent to interested actors.")

  // Register async callback to be fired when all Spark tasks are fully registered by the actors above
  readyFuture onSuccess {
    case (f1: Ready, f2: Ready) =>
      Logger.info("Spark actions registered. Starting Spark context.")

      // Send batches of tweets to the indexer
      streamHandle.foreachRDD(batch => {
        indexer ! TweetBatch(batch.collect().toList)
      })

      // Actors have registered Spark actions, so we can initialise the Spark context
      streamHandle.context.start()
      streamHandle.context.awaitTermination()
  }


  override def receive: Actor.Receive = {
    case _ => Logger.error("No message receipt actions defined for TweetStreamActor")
  }

}
