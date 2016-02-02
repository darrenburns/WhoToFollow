package persist.actors

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.Inject
import com.google.inject.name.Named
import com.mongodb.casbah.commons.MongoDBObject
import hooks.MongoInit
import learn.actors.FeatureExtraction.UserFeatures
import persist.actors.RedisQueryWorker.UserFeatureRequest
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object LabelStore {

  object VoteType extends Enumeration {
    type VoteType = Value
    val LOW_QUALITY, HIGH_QUALITY = Value
  }

  case class Vote(screenName: String, query: String, isRelevant: Boolean)
  implicit val voteReads: Reads[Vote] = (
    (JsPath \ "screenName").read[String] and
      (JsPath \ "query").read[String] and
      (JsPath \ "isRelevant").read[Boolean]
    )(Vote.apply _)

  lazy val db = MongoInit.db
  lazy val collection = db("labels")
}


/**
  * Receives votes and stores them in the database
  */
class LabelStore @Inject() (
  @Named("redisActor") redisActor: ActorRef
) extends Actor {

  import LabelStore._

  implicit val timeout = Timeout(20 seconds)

  def receive = {

    case Vote(name, queryString, isRelevant) =>
      Logger.debug("LabelStore received new vote. Will now look up Redis for user features.")
      // Fetch the features for this user from Redis
      (redisActor ? UserFeatureRequest(name)) onComplete {
        case Success(features: UserFeatures) =>
          val query = MongoDBObject(
            "name" -> features.screenName,
            "query" -> queryString
          )
          val dbVote = MongoDBObject(
            "name" -> features.screenName,
            "query" -> queryString,
            "isRelevant" -> isRelevant,
            "tweetCount" -> features.tweetCount,
            "followerCount" -> features.followerCount,
            "wordCount" -> features.wordCount,
            "capitalisedCount" -> features.capitalisedCount,
            "hashtagCount" -> features.hashtagCount,
            "retweetCount" -> features.retweetCount,
            "likeCount" -> features.likeCount,
            "dictionaryHits" -> features.dictionaryHits,
            "linkCount" -> features.linkCount
          )
          Logger.debug("Features found for user. Saving features and classification in database.")
          collection.update(query, dbVote, upsert=true)
          sender ! Success
        case Failure(t) =>
          Logger.debug(s"Failed to fetch user features for '$name' from Redis. Error: $t")
          sender ! Failure
      }
  }

}
