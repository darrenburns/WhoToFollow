package hooks

import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import com.mongodb.casbah.MongoClient
import com.redis.RedisClientPool
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import twitter4j.TwitterFactory


object SparkInit {
  val conf = new SparkConf().setMaster("local[2]").setAppName("WTFContext")
  lazy val ssc = new StreamingContext(conf, Seconds(1))
}

object RedisConnectionPool {
  lazy val pool = new RedisClientPool("localhost", 6379)
}

object MongoInit {
  private lazy val mc = MongoClient("localhost", 27017)
  lazy val db = mc("wtfcontext")
}

object Twitter {
  lazy val instance = TwitterFactory.getSingleton
}


@Singleton
class ApplicationPreStart @Inject()(system: ActorSystem,
                                    @Named("tweetStreamActor") tweetStreamActor: ActorRef) {



}


