package actors

import actors.WordCountActor.ActiveTwitterStream
import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import init.{SparkInit, TwitterAuth}
import org.apache.spark.streaming.twitter.TwitterUtils
import play.api.libs.json.{Json, Writes}
import twitter4j.Status

case class TweetBatch(tweets: List[Status])

object TweetStreamActor {
  val DefaultBatchSize = 20

}

@Singleton
class TweetStreamActor @Inject()
  (@Named("webSocketSupervisor") webSocketSupervisor: ActorRef,
   @Named("wordCountActor") wordCountActor: ActorRef)
  extends Actor with TwitterAuth {

  import TweetStreamActor._

  println("Initialising TweetStreamActor")
  checkTwitterKeys()
  val streamHandle = TwitterUtils.createStream(SparkInit.ssc, None)
  wordCountActor ! ActiveTwitterStream(streamHandle)
  sendTweetBatches()

  override def receive: Actor.Receive = ???

  def sendTweetBatches(): Unit = {
    streamHandle.foreachRDD(rdd => {
      webSocketSupervisor ! TweetBatch(rdd.take(DefaultBatchSize).toList)
    })
  }

}
