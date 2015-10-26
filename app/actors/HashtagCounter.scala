package actors

import actors.PipelineSupervisor.HashtagCountUpdate
import akka.actor.{Actor, ActorRef}
import com.fasterxml.jackson.databind.JsonNode
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.slf4j.LoggerFactory
import play.api.libs.json.{Json, Writes}
import play.mvc.WebSocket
import twitter4j.Status

object HashtagCounter {
  val logger = LoggerFactory.getLogger(getClass)
  case class ActiveTwitterStream(dStream: ReceiverInputDStream[Status])
  case class ActivateOutputStream(out: WebSocket.Out[JsonNode])
  case class HashtagCount(hashtag: String, count: Int)

  implicit val hashtagCountWrites = new Writes[HashtagCount] {
    def writes(hashtagCount: HashtagCount) = Json.obj(
      "hashtag" -> hashtagCount.hashtag,
      "count" -> hashtagCount.count
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
class HashtagCounter @Inject() (@Named("pipelineSupervisor") supervisor: ActorRef) extends Actor {
  import actors.HashtagCounter._

  override def receive = {
    case ActiveTwitterStream(stream) => processStream(stream)  // Initialise
    case _ => logger.debug("WordCountActor received an unrecognised request.")
  }

  def processStream(stream: ReceiverInputDStream[Status]): Unit = {
    // Tuples of (user, hashtag)
    val sc = stream.context.sparkContext
    // What do we want to do with each status?
    /*
    We want to map each status into a list of tuples of ((user, hashtag), count)
    We don't care about the source status of each hashtag here, so it should be flatMap.
    Then reduceByKey
     */
    val userHashtags = stream.flatMap(status => {
      status.getText.split(" ")
        .filter(_.startsWith("#"))
        .map(hashtag => ((status.getUser.getScreenName, hashtag), 1))
    })

//      (status.getUser.getName, status.getText.split(" ").filter(_.startsWith("#"))))


    val userHashtagCounts = userHashtags.foreachRDD(uht => {
//      user.mapValues(list => ))
      val tags = uht.collect()
      tags.foreach(tag =>
        println("User: " + tag._1._1 + ", hashtag: " + tag._1._2 + ", count: " + tag._2)
      )
    })
//      .reduceByKeyAndWindow((p: Int, q: Int) => p + q, Seconds(10), Seconds(10))
//      .transform(_.sortByKey(ascending = false))


//    userHashtagCounts.foreachRDD(rdd => {
//      println(rdd.take(3).toList)
//      supervisor ! HashtagCountUpdate(rdd.collect().toList)
//    })
    // TODO: Probably need to move this.
    stream.context.start()
    stream.context.awaitTermination()
  }



}

