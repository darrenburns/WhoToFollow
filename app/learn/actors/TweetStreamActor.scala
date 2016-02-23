package learn.actors

import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import akka.pattern.ask
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import hooks.SparkInit
import learn.actors.TweetStreamActor.{Ready, TweetBatch}
import learn.actors.UserHashtagCounter.ActiveTwitterStream
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.twitter.TwitterUtils
import play.api.{Configuration, Logger}
import twitter4j.Status
import utils.TwitterAuth

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object TweetStreamActor {
  case class TweetBatch(tweets: Seq[Status])
  case class Ready()
}

@Singleton
class TweetStreamActor @Inject()
(
   @Named("userHashtagCounter") userHashtagCounter: ActorRef,
   @Named("featureExtraction") featureExtraction: ActorRef,
   @Named(Indexer.name) indexer: ActorRef,
   config: Configuration
)
  extends Actor with TwitterAuth {

  implicit val timeout = Timeout(20 seconds)

  Logger.info("Initialising TweetStreamActor")

  val batchSize = config.getInt("stream.sourcefile.batchSize").getOrElse(10)
  val batchDuration = config.getInt("stream.sourcefile.batchDuration").getOrElse(4)

  val streamHandle: ReceiverInputDStream[Status] =
    config.getString("stream.sourcefile.path") match {
      case Some(sourceFile) =>
        // If we want to simulate a Twitter stream using a JSON file
        SparkInit.ssc.actorStream[Status](TweetStreamSimulator.props[Status](sourceFile, batchSize, batchDuration), TweetStreamSimulator.name)
      case None =>
        // If there is no file that we are using as our source, default to the Twitter sample stream
        checkTwitterKeys()
        TwitterUtils.createStream(SparkInit.ssc, None)
  }

//  // TODO: Remove these after debug
//  // Send batches of tweets to the indexer
//  streamHandle.foreachRDD(batch => {
//    indexer ! TweetBatch(batch.collect().toList)
//  })
//  streamHandle.context.start()
//  streamHandle.context.awaitTermination()

//   Asynchronously send status stream handle to interested actors
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
