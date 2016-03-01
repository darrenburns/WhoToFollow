package learn.actors

import java.io._
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

import akka.actor.{Actor, PoisonPill, Props}
import com.google.inject.assistedinject.Assisted
import com.google.inject.{Inject, Singleton}
import di.NamedActor
import jawn.AsyncParser
import jawn.ast.JParser
import learn.actors.TweetStreamActor.TweetBatch
import org.apache.spark.streaming.receiver.ActorHelper
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

  val getStatusTick = context.system.scheduler.schedule(Duration.Zero,
    FiniteDuration(batchDuration, TimeUnit.MILLISECONDS), self, TakeNextStatusBatch)

  val parser = JParser.async(mode = AsyncParser.UnwrapArray)
  val tweetIterator = Source.fromInputStream(gis(sourceFile)).getLines()

  override def receive = {
    case TakeNextStatusBatch =>
      self ! takeNextStatusBatch()
    case TweetBatch(statusList) =>
      statusList.foreach(store(_))
      // When we finish reading the file
      if (statusList.length < batchSize) {
        self ! PoisonPill
      }
  }

  def takeNextStatusBatch(): TweetBatch = {
    val statusBatch = tweetIterator.take(batchSize)
    val statusList = statusBatch.map(status => {
      DataObjectFactory.createStatus(status)
    })
    TweetBatch(statusList.toSeq)
  }

  def gis(s: String) = new GZIPInputStream(new BufferedInputStream(new FileInputStream(s)))

}
