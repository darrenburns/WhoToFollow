package actors

import actors.TweetStreamActor.Ready
import actors.UserHashtagCounter.ActiveTwitterStream
import actors.UserLanguageModelling.{UserModelUpdate, UserWordCount}
import akka.actor.{ActorRef, Actor}
import com.google.inject.name.Named
import com.google.inject.{Singleton, Inject}
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import play.api.Configuration
import twitter4j.Status


object UserLanguageModelling {

  object Defaults {
    val WindowSize = 10
  }

  case class UserWordCount(username: String, word: String, count: Int)
  case class UserModelUpdate(counts: Seq[UserWordCount])

}

@Singleton
class UserLanguageModelling @Inject()
  (config: Configuration,
   @Named("userLanguageModelWriter") userLanguageModelWriter: ActorRef)
  extends Actor {

  val reportFrequency = config.getInt("analysis.counts.windowSize")
    .getOrElse(UserLanguageModelling.Defaults.WindowSize)

  override def receive = {
    case ActiveTwitterStream(stream) =>
      countUserWords(stream)
      sender ! Ready()
  }

  def countUserWords(stream: ReceiverInputDStream[Status]) = {
    val userWords = stream.flatMap(status => {
      status.getText.split(" ")
        .map(word => ((status.getUser.getScreenName, word toLowerCase()), 1))
    })

    val wordCountInWindow = userWords
      .reduceByKeyAndWindow((p: Int, q: Int) => p+q, Seconds(reportFrequency), Seconds(reportFrequency))
      .map{case ((user, word), count) => (count, (user, word))}
      .map{case (count, (user, word)) => UserWordCount(user, word, count)}

    wordCountInWindow.foreachRDD(rdd => {
      val userWordCounts = rdd.collect()
      userLanguageModelWriter ! UserModelUpdate(userWordCounts)
    })

  }

}
