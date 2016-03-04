package learn.actors

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import channels.actors.MetricsReporting
import channels.actors.MetricsReporting.NumberOfUsersSeen
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import hooks.SparkInit
import learn.actors.BatchFeatureExtraction.SetCurrentMaxStatusId
import learn.actors.TweetStreamActor.PipelineActorReady
import learn.actors.UserHashtagCounter.ActiveTwitterStream
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.twitter.TwitterUtils
import play.api.{Configuration, Logger}
import twitter4j.Status
import utils.TwitterAuth

import scala.collection.immutable.HashSet
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object TweetStreamActor {
  case class TweetBatch(tweets: Seq[Status])
  case class PipelineActorReady()
}

@Singleton
class TweetStreamActor @Inject()
(
   @Named("userHashtagCounter") userHashtagCounter: ActorRef,
   @Named("featureExtraction") featureExtraction: ActorRef,
   @Named(BatchFeatureExtraction.name) batchFeatureExtraction: ActorRef,
   @Named(MetricsReporting.name) metricsReporting: ActorRef,
   @Named(Indexer.name) indexer: ActorRef,
   config: Configuration
)
  extends Actor with TwitterAuth {

  implicit val timeout = Timeout(20 seconds)

  Logger.info("Initialising TweetStreamActor")

  val batchSize = config.getInt("stream.sourcefile.batchSize").getOrElse(10)
  val batchDuration = config.getInt("stream.sourcefile.batchDuration").getOrElse(4)

  var screenNamesSeen = HashSet[String]()

  checkTwitterKeys()
  val streamHandle: ReceiverInputDStream[Status] =
    config.getString("stream.sourcefile.path") match {
      case Some(sourceFile) =>
        // If we want to simulate a Twitter stream using a JSON file
        SparkInit.ssc.actorStream[Status](TweetStreamSimulator.props[Status](sourceFile, batchSize, batchDuration), TweetStreamSimulator.name)
      case None =>
        // If there is no file that we are using as our source, default to the Twitter sample stream
        TwitterUtils.createStream(SparkInit.ssc, None)
  }

  // Asynchronously send status stream handle to interested actors
  val readyFuture = for {
    f1 <- userHashtagCounter ? ActiveTwitterStream(streamHandle)
    f2 <- featureExtraction ? ActiveTwitterStream(streamHandle)
  } yield (f1, f2)

  Logger.info("Stream handle sent to interested actors.")

  // Register async callback to be fired when all Spark tasks are fully registered by the actors above
  readyFuture onSuccess {
    case (f1: PipelineActorReady, f2: PipelineActorReady) =>
      Logger.info("Spark actions registered. Starting Spark context.")

      // Send batches of tweets to the expecting actors
      streamHandle.foreachRDD(batch => {
        val statusList = batch.collect().toList
        screenNamesSeen = screenNamesSeen ++ statusList.map(_.getUser.getScreenName)
        metricsReporting ! NumberOfUsersSeen(screenNamesSeen.size)
        if (statusList.nonEmpty) {
          batchFeatureExtraction ! SetCurrentMaxStatusId(statusList.last.getId)
        }
      })

      // Actors have registered Spark actions, so we can initialise the Spark context
      streamHandle.context.start()
      streamHandle.context.awaitTermination()
  }


  override def receive: Actor.Receive = {
    case _ => Logger.error("No message receipt actions defined for TweetStreamActor")
  }

}
