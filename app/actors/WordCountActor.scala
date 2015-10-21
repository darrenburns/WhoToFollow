package actors

import actors.PipelineSupervisor.WordCountUpdate
import akka.actor.{Actor, ActorRef}
import com.fasterxml.jackson.databind.JsonNode
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.{DStream, ReceiverInputDStream}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsNumber, Writes, Json}
import play.mvc.WebSocket
import twitter4j.Status

object WordCountActor {
  val logger = LoggerFactory.getLogger(getClass)
  case class ActiveTwitterStream(dStream: ReceiverInputDStream[Status])
  case class ActivateOutputStream(out: WebSocket.Out[JsonNode])
  case class WordCount(word: String, count: Int)

  implicit val wordCountWrites = new Writes[WordCount] {
    def writes(wordCount: WordCount) = Json.obj(
      "word" -> wordCount.word,
      "count" -> wordCount.count
    )
  }
}


/*
 TODO: this should do nothing other than run in the
 background after starting, and send messages. it is
 permanently occupied and cannot receive messages
 while blocked
 */

@Singleton
class WordCountActor @Inject() (@Named("pipelineSupervisor") supervisor: ActorRef) extends Actor {
  import actors.WordCountActor._

  override def receive = {
    case ActiveTwitterStream(stream) => processStream(stream)  // Initialise
    case _ => logger.debug("WordCountActor received an unrecognised request.")
  }

  def processStream(stream: ReceiverInputDStream[Status]): Unit = {
    val hashTags = stream.flatMap(status => status.getText.split(" ")).filter(_.startsWith("#"))
    val counts = getWordCountStreamInWindow(hashTags, 10, 10)  // TODO: Get stream window parameters from config
    counts.foreachRDD(rdd => {
      supervisor ! WordCountUpdate(rdd.take(10).toList)
    })
    // TODO: Probably need to move this.
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

