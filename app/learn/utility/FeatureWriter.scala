package learn.utility

import java.io.{FileWriter, BufferedWriter, File}
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, Actor}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import learn.actors.FeatureExtraction.UserFeatures
import learn.utility.FeatureWriter.WriteFeaturesToLetor
import persist.actors.LabelStore.GetLatestTrainingData
import persist.actors.{LabelStore, RedisActor}

import scala.concurrent.duration.{FiniteDuration, Duration}
import scala.concurrent.ExecutionContext.Implicits.global

/*
screenName: String,
                           tweetCount: Int,
                           followerCount: Int,
                           wordCount: Int,
                           // TODO                  punctuationCounts: Map[String, Int],  Temporarily disabled
                           capitalisedCount: Int,
                           hashtagCount: Int,
                           retweetCount: Int,
                           likeCount: Int,
                           dictionaryHits: Int,
                           linkCount: Int,
                           hashtagTimestamps: List[(String, Double)]
 */

object FeatureWriter {
  case class WriteFeaturesToLetor(features: UserFeatures)
}

@Singleton
class FeatureWriter @Inject() (@Named(LabelStore.name) labelStore: ActorRef) extends Actor {

  context.system.scheduler.schedule(Duration.Zero, FiniteDuration(1, TimeUnit.MINUTES),
    labelStore, GetLatestTrainingData)
  def receive = {
    case WriteFeaturesToLetor(f) =>
      val file = new File("conf/features.letor")
      val writer = new BufferedWriter(new FileWriter(file))
      writer.write("# 1:tweetCount 2:wordCount 3:capitalisedCount 4:hashtagCount 5:retweetCount 6:likeCount 7:dictionaryHits 8:linkCount\n")
      writer.write(s"1 NA 1:${f.tweetCount} 2:${f.wordCount} 3:${f.capitalisedCount} 4:${f.hashtagCount} 5:${f.retweetCount} 6:${f.likeCount} 7:${f.dictionaryHits} 8:${f.linkCount}\n")
      writer.close()
  }

}
