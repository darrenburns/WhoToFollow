package learn.actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, PoisonPill, Props}
import com.google.inject.assistedinject.Assisted
import com.google.inject.{Inject, Singleton}
import di.NamedActor
import learn.actors.TweetStreamActor.TweetBatch
import org.apache.spark.streaming.receiver.ActorHelper
import play.api.Logger
import play.api.libs.json.{JsArray, Json}
import twitter4j.json.DataObjectFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.io.Source
import scala.reflect.ClassTag


object TweetStreamSimulator extends NamedActor {
  override final val name = "TweetStreamSimulator"
  case object TakeNextStatusBatch

  def props[T: ClassTag](sourceFile: String, batchSize: Int, batchDuration: Int): Props =
    Props(new TweetStreamSimulator[T](sourceFile, batchSize, batchDuration))
}

@Singleton
class TweetStreamSimulator[T: ClassTag] @Inject()
(
  @Assisted sourceFile: String,
  @Assisted batchSize: Int,
  @Assisted batchDuration: Int
) extends Actor with ActorHelper {

  import TweetStreamSimulator._

  var startIdx = 0
  val tweetsRawJson = Source.fromFile(sourceFile).getLines.mkString("\n")
  val tweetsJson = Json.parse(tweetsRawJson) match {
    case tweets: JsArray =>
      Logger.debug("[CAPTURE] Successfully matched tweets from file!")
      tweets.value.map(status => status.toString())
  }

  Logger.debug("Number of tweets: " + tweetsJson.size)
  tweetsJson.foreach(Logger.debug(_))

  val getStatusTick = context.system.scheduler.schedule(Duration.Zero,
    FiniteDuration(batchDuration, TimeUnit.MILLISECONDS), self, TakeNextStatusBatch)

  override def receive = {
    case TakeNextStatusBatch =>
      self ! takeNextStatusBatch(startIdx, startIdx + batchSize)
      startIdx += 1
    case TweetBatch(statusList) =>
      Logger.debug("[CAPTURE] StatusList received: " + statusList)
      statusList.foreach(store(_))
      // When we finish reading the file
      if (statusList.length < batchSize) {
        self ! PoisonPill
      }
  }

  private def takeNextStatusBatch(startIdx: Int, endIdx: Int): TweetBatch = {
    val statusBatch = if (endIdx >= tweetsJson.size) tweetsJson.drop(startIdx - 1)
                      else tweetsJson.slice(startIdx, endIdx)
    val statusList = statusBatch.map(status => {
      DataObjectFactory.createStatus(status)
    })
    TweetBatch(statusList)
  }


}
