package actors

import akka.actor.{ActorSystem, Actor, ActorRef, Props}
import com.google.inject.Inject
import com.google.inject.name.Named
import models.WordCount
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.{DStream, ReceiverInputDStream}
import org.slf4j.LoggerFactory
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import twitter4j.Status

import play.api.Play.current

import scala.collection.immutable.HashSet
import scala.collection.mutable.ListBuffer

object WordCountActor {

  val logger = LoggerFactory.getLogger(getClass)
  lazy val wordCountActor: ActorRef = Akka.system.actorOf(Props(classOf[WordCountActor]))

  case class ActiveTwitterStream(dStream: ReceiverInputDStream[Status])
}

case class ResultUpdate(results: List[WordCount])

class WordCountActor @Inject() (actorSystem: ActorSystem,
                                 @Named("userWatcherActor") userWatcherActor: ActorRef) extends Actor {
  import actors.WordCountActor._
//
//  implicit val wordCountWrites = new Writes[WordCount] {
//    def writes(wordCount: WordCount) = Json.obj(
//      "word" -> wordCount.word,
//      "count" -> wordCount.count
//    )
//  }

  override def receive = {
    case ActiveTwitterStream(stream) => processStream(stream)  // Initialise
    case _ => logger.debug("WordCountActor received a non-valid Twitter stream ")
  }

  /*
     For now we define processing the stream to be simply printing hashtags to the console.
     */
  def processStream(stream: ReceiverInputDStream[Status]): Unit = {
    logger.debug("Processing Twitter stream")
    println("Inside processStream")
    val hashTags = stream.flatMap(status => status.getText.split(" ")).filter(_.startsWith("#"))
    val counts = getWordCountStreamInWindow(hashTags, 10, 10)  // TODO: Get stream window parameters from config
    println("Got counts")
    counts.foreachRDD(rdd => {
      userWatcherActor ! ResultUpdate(rdd.take(10).toList)
    })
    println("Calling stream.context.start()")
    stream.context.start()
    stream.context.awaitTermination()
  }

  /**
   * Count the number of occurrences of each word.
   * @param stream The DStream of input words to count occurrences of
   * @param windowWidth The number of seconds to keep track of occurrences for
   * @param outputFrequency The frequency at which the occurrences are output
   * @return A DStream of tuples of the form (word, numberOfOccurrences)
   */
  def getWordCountStreamInWindow(stream: DStream[String], windowWidth: Int,
                                 outputFrequency: Int): DStream[WordCount] = {
    stream.map((_, 1))
      .reduceByKeyAndWindow((p:Int, q:Int) => p + q, Seconds(windowWidth), Seconds(outputFrequency))
      .map{ case (hashTag, count) => (count, hashTag) }  // Swap so that we can sort by key
      .transform(_.sortByKey(ascending = false)
      .map{ case (count, hashTag) => new WordCount(hashTag, count) })  // Revert back to original format of (hashTag, count)
  }

}

