package actors

import actors.TweetStreamActor.Ready
import actors.UserHashtagCounter.ActiveTwitterStream
import akka.actor.Status.Success
import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import init.{SparkInit, TwitterAuth}
import org.apache.spark.streaming.twitter.TwitterUtils
import play.api.Logger
import twitter4j.Status

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.Failure


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
   @Named("userIndexing") userIndexing: ActorRef
)
  extends Actor with TwitterAuth {

  implicit val timeout = Timeout(20 seconds)

  Logger.info("Initialising TweetStreamActor")
  checkTwitterKeys()
  val streamHandle = TwitterUtils.createStream(SparkInit.ssc, None)

  // Asynchronously send status stream handle to interested actors
  val responses = for {
    f1 <- userHashtagCounter ? ActiveTwitterStream(streamHandle)
    f2 <- featureExtraction ? ActiveTwitterStream(streamHandle)
    f3 <- userIndexing ? ActiveTwitterStream(streamHandle)
  } yield (f1, f2, f3)

  Logger.info("Stream handle sent to interested actors.")

  // We have to block here because we must ensure that the Spark context
  // is not started until the actors above have registered their Spark actions
  responses onSuccess {
    case (f1: Ready, f2: Ready, f3: Ready) =>
    Logger.info("Spark actions registered. Starting Spark context.")

    // Actors have registered Spark actions, so we can initialise the Spark context
    streamHandle.context.start()
    streamHandle.context.awaitTermination()
  }


  override def receive: Actor.Receive = {
    case _ => Logger.error("No message receipt actions defined for TweetStreamActor")
  }

}
