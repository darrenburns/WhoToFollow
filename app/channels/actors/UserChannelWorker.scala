package channels.actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, PoisonPill}
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import com.google.inject.name.Named
import learn.actors.FeatureExtraction.UserFeatures
import org.joda.time.DateTime
import persist.actors.RedisActor
import persist.actors.RedisQueryWorker.{SendUserFeaturesToChannel, GetUserFeatures}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.{Configuration, Logger}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Success}


object UserChannelWorker {

  trait Factory extends GenericChannel.Factory

  object Defaults {
    val Expiry = 15000
  }

  case object PushLatestUserFeatures

  implicit val userFeaturesWrites = new Writes[UserFeatures] {
    def writes(f: UserFeatures) = {
      val hashtagTimestamps = Json.toJson(f.hashtagTimestamps.map(hashtagTs => {
        Json.obj("hashtag" -> hashtagTs._1, "timestamp" -> hashtagTs._2)
      }))
      Json.obj(
        "screenName" -> f.screenName,
        "tweetCount" -> f.tweetCount,
        "followerCount" -> f.followerCount,
        "wordCount" -> f.wordCount,
        "capitalisedCount" -> f.capitalisedCount,
        "hashtagCount" -> f.hashtagCount,
        "retweetCount" -> f.retweetCount,
        "likeCount" -> f.likeCount,
        "dictionaryHits" -> f.dictionaryHits,
        "linkCount" -> f.linkCount,
        "hashtagTimestamps" -> hashtagTimestamps
      )
    }
  }

}

class UserChannelWorker @Inject() (
  @Assisted screenName: String,
  @Assisted channel: Concurrent.Channel[JsValue],
  @Assisted parent: ActorRef,
  config: Configuration, @Named(RedisActor.name) redisActor: ActorRef,
  @Named(UserChannelSupervisor.name) userChannelSupervisor: ActorRef
) extends Actor with GenericChannel.Worker {

  import UserChannelWorker._

  Logger.debug(s"UserChannelWorker [${self.path}] started.")

  implicit val timeout = Timeout(10, TimeUnit.SECONDS)
  override val channelName = screenName
  override val channelExpiry = config.getInt("channels.user.expiry").getOrElse(Defaults.Expiry)
  override var latestKeepAlive = DateTime.now

  val pushTick =
    context.system.scheduler.schedule(Duration.Zero, FiniteDuration(2, TimeUnit.SECONDS), self, PushLatestUserFeatures)
  val expiryTick =
    context.system.scheduler.schedule(Duration.Zero, FiniteDuration(30, TimeUnit.SECONDS), self, CheckExpired)

  override def receive = {
    case KeepAlive => handleKeepAlive()
    case PushLatestUserFeatures => pushLatest()
    case CheckExpired => if (hasExpired) suicideAndCleanup(userChannelSupervisor)
    case features: UserFeatures => channel push Json.toJson(features)
  }

  override def pushLatest(): Unit = {
   redisActor ! SendUserFeaturesToChannel(screenName)
  }

  override def postStop() = {
    pushTick.cancel()
    expiryTick.cancel()
  }

}
