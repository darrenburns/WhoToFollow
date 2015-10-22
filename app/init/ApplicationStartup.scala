package init


import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

object SparkInit {
  val conf = new SparkConf().setMaster("local[2]").setAppName("WTFContext")
  lazy val ssc = new StreamingContext(conf, Seconds(1))
}

@Singleton
class ApplicationStartup @Inject() (system: ActorSystem,
                                     @Named("tweetStreamActor") tweetStreamActor: ActorRef) {


}


