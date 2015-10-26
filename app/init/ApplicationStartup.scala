package init

import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import com.redis.RedisClient
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}


object SparkInit {
  val conf = new SparkConf().setMaster("local[2]").setAppName("WTFContext")
  lazy val ssc = new StreamingContext(conf, Seconds(1))
}

object RedisInit {
  lazy val redis = new RedisClient("localhost", 6379)
}

@Singleton
class ApplicationStartup @Inject() (system: ActorSystem,
                                     @Named("tweetStreamActor") tweetStreamActor: ActorRef) {


}


