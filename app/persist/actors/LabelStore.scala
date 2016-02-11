package persist.actors

import java.util.Date

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.{Singleton, Inject}
import com.google.inject.name.Named
import com.mongodb.WriteResult
import com.mongodb.casbah.commons.MongoDBObject
import hooks.MongoInit
import learn.actors.FeatureExtraction.UserFeatures
import persist.actors.RedisQueryWorker.GetUserFeatures
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object LabelStore {

  implicit val voteReads: Reads[UserRelevance] = (
    (JsPath \ "screenName").read[String] and
      (JsPath \ "query").read[String] and
      (JsPath \ "isRelevant").read[Boolean]
    )(UserRelevance.apply _)

  lazy val db = MongoInit.db
  lazy val collection = db("labels")

  sealed trait LabelStoreResponse
  case class UserRelevance(screenName: String, query: String, isRelevant: Boolean) extends LabelStoreResponse
  case object NoUserRelevanceDataOnRecord extends LabelStoreResponse

  case class GetUserRelevance(screenName: String, query: String)
}


/**
  * Receives votes and stores them in the database
  */
@Singleton
class LabelStore @Inject() (
  @Named(RedisActor.name) redisActor: ActorRef
) extends Actor {

  import LabelStore._

  implicit val timeout = Timeout(20 seconds)

  def receive = {
    case UserRelevance(name, queryString, isRelevant) =>
      voteForUser(name, queryString, isRelevant)
    case GetUserRelevance(screenName, query) =>
      sender ! getUserRelevance(screenName, query)
  }

  private def voteForUser(screenName: String, queryString: String, isRelevant: Boolean): Unit = {
    // Fetch the features for this user from Redis
    (redisActor ? GetUserFeatures(screenName)) onComplete {
      case Success(features: UserFeatures) =>
        val dbVote = MongoDBObject(
          "timestamp" -> new Date(),
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
        Logger.debug(s"Saving classification: ($screenName, $queryString, isRelevant: $isRelevant).")
        collection.insert(dbVote)
        sender ! Success  // TODO: Currently assuming success instead of matching on WriteResult
      case Failure(t) =>
        Logger.debug(s"Failed to fetch user features for '$screenName' from Redis. Error: $t")
        sender ! Failure
    }

  }

  private def getUserRelevance(screenName: String, query: String): LabelStoreResponse = {
    val queryObject = MongoDBObject("name" -> screenName, "query" -> query)
    collection.findOne(queryObject) match {
      case Some(userRelevanceObj: collection.T) =>
        val rel = userRelevanceObj.toMap
        UserRelevance(
          screenName = rel.get("name").asInstanceOf[String],
          query = rel.get("query").asInstanceOf[String],
          isRelevant = rel.get("isRelevant").asInstanceOf[Boolean]
        )
      case None =>
        NoUserRelevanceDataOnRecord
    }
  }

}
