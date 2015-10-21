package init


import actors.WordCountActor.ActiveTwitterStream
import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.{Inject, Singleton}
import com.google.inject.name.Named
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import twitter4j.Status

object SparkInit {
  val conf = new SparkConf().setMaster("local[2]").setAppName("WTFContext")
  lazy val ssc = new StreamingContext(conf, Seconds(1))
}

@Singleton
class ApplicationStartup @Inject() (system: ActorSystem,
                                     @Named("pipelineSupervisor") pipelineSupervisor: ActorRef) extends TwitterAuth {

  val streamHandle: ReceiverInputDStream[Status] = initialise()
  pipelineSupervisor ! ActiveTwitterStream(streamHandle)

  def initialise(): ReceiverInputDStream[Status] = {
    println("Initialising")
    checkTwitterKeys()
    TwitterUtils.createStream(SparkInit.ssc, None)
  }

}


