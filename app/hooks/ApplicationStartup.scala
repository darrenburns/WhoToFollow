package hooks

import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import com.mongodb.casbah.MongoClient
import com.redis.{RedisClientPool, RedisClient}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}


object SparkInit {
  val conf = new SparkConf().setMaster("local[2]").setAppName("WTFContext")
  lazy val ssc = new StreamingContext(conf, Seconds(1))
}

object RedisConnectionPool {
  lazy val pool = new RedisClientPool("localhost", 6379)
}

object MongoInit {
  private lazy val mc = MongoClient("localhost", 27017)
  private lazy val db = mc("wtfcontext")
  lazy val coll = db("labels")

}

@Singleton
class ApplicationStartup @Inject() (system: ActorSystem,
                                     @Named("tweetStreamActor") tweetStreamActor: ActorRef) {

  // Inject the tweetStream to initialise it on application startup

}


