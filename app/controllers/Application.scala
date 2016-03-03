package controllers

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.actor.Status.Success
import akka.pattern.ask
import akka.util.Timeout
import channels.actors.ChannelMessages.CreateChannel
import channels.actors.MetricsReporting.GetMetricsChannel
import channels.actors.{MetricsReporting, UserChannelSupervisor, QuerySupervisor}
import com.github.nscala_time.time.Imports._
import com.google.inject.Inject
import com.google.inject.name.Named
import hooks.Twitter
import learn.actors.BatchFeatureExtraction
import learn.actors.BatchFeatureExtraction.GetCurrentMaxStatusId
import learn.actors.TweetStreamActor.TweetBatch
import persist.actors.LabelStore
import persist.actors.LabelStore.{GetUserRelevance, NoUserRelevanceDataOnRecord, UserRelevance}
import persist.actors.UserMetadataReader.UserMetadata
import persist.actors.UserMetadataWriter.UserMetadataQuery
import play.api.Logger
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc._
import query.actors.QueryService.Query
import twitter4j.{Paging, Status}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

object Application {

  implicit val statusWrites = new Writes[Status] {
    def writes(status: Status) = {
      val retweetedStatus = status.getRetweetedStatus
      val statusJson = Json.obj(
        "id" -> status.getId,
        "text" -> status.getText,
        "username" -> status.getUser.getName,
        "screenname" -> status.getUser.getScreenName,
        "date" -> new DateTime(status.getCreatedAt).getMillis,
        "retweets" -> status.getRetweetCount,
        "likes" -> status.getFavoriteCount,
        "avatar" -> status.getUser.getProfileImageURL,
        "isRetweet" -> status.isRetweet
      )
      if (status.isRetweet) {
        // If it's a retweet include the original author in the JSON
        val retweetedUser = retweetedStatus.getUser
        statusJson ++ Json.obj(
          "retweetedUser" -> Json.obj(
            "username" -> retweetedUser.getName,
            "screenname" -> retweetedUser.getScreenName
          )
        )
      } else {
        // Otherwise just write JSON for the status
        statusJson
      }
    }
  }

  implicit val userRelevanceWrites = new Writes[UserRelevance] {
    def writes(rel: UserRelevance) = {
      Json.obj("screenName" -> rel.screenName, "query" -> rel.query, "isRelevant" -> rel.isRelevant)
    }
  }

}

class Application @Inject()
(
  @Named(UserChannelSupervisor.name) userChannelSupervisor: ActorRef,
  @Named(QuerySupervisor.name) querySupervisor: ActorRef,
  @Named(MetricsReporting.name) metricsReporting: ActorRef,
  @Named(LabelStore.name) labelStore: ActorRef,
  @Named(BatchFeatureExtraction.name) batchFeatureExtraction: ActorRef,
  @Named("userMetadataReader") userMetadataReader: ActorRef
) extends Controller {

  import Application._

  implicit val timeout = Timeout(10, TimeUnit.SECONDS)

  /**
    * Display the index static file which contains the mount point for the frontend React app.
    * Routing is handled client side, and all data is retrieved via Ajax, so this is the only
    * view that is used.
    * @return An HTTP 200 OK response containing the application's index page.
    */
  def index = Action {
    Ok(views.html.index())
  }

  /**
    * Receives a GET request containing a query. This method will request that the WSS creates
    * a new channel named after the query.
    */
  def getQueryChannel(queryString: String) = WebSocket.tryAccept[JsValue] { request =>
      (querySupervisor ? CreateChannel(queryString))
        .mapTo[Either[Result, (Iteratee[JsValue, _], Enumerator[JsValue])]]
  }

  /**
    * Receives a GET request containing a screenName. This method will upgrade the connection
    * to WebSocket which handles sending the latest user features to the client.
    */
  def getUserChannel(screenName: String) = WebSocket.tryAccept[JsValue] { request =>
    (userChannelSupervisor ? CreateChannel(screenName))
      .mapTo[Either[Result, (Iteratee[JsValue, _], Enumerator[JsValue])]]
  }

  def getMetricsChannel = WebSocket.tryAccept[JsValue] { request =>
    (metricsReporting ? GetMetricsChannel)
      .mapTo[Either[Result, (Iteratee[JsValue, _], Enumerator[JsValue])]]
  }

  /**
    * Receives a POST request containing a Vote object which
    * is sent to the store for future machine learning tasks.
    * @return An HTTP 200 response
    */
  def rateUser = Action(BodyParsers.parse.json) { request =>
    val json = request.body.validate[UserRelevance]
    json.fold(
      errors => {
        BadRequest
      },
      vote => {
        Logger.info(s"Received vote: $vote" )
        labelStore ! vote
        Ok
      }
    )
  }

  /**
    * whose timeline we are to return and also analyse.
    *
    * @return An HTTP response with a JSON object containing a list of the tweets present in the timeline
    *         of the user contained within the request parameters.
    */
  def fetchAndAnalyseTimeline(screenName: String) = Action.async { request =>

    // Fetch a list of tweets from the users timeline
    val twitter = Twitter.instance
    (batchFeatureExtraction ? GetCurrentMaxStatusId).mapTo[Long].flatMap {
      case maxStatusId: Long =>
        val paging = new Paging()
        paging.setMaxId(maxStatusId)
        paging.setCount(20)
        val tweets = twitter.getUserTimeline(screenName, paging)
        if (tweets.nonEmpty) {
          Logger.debug(s"Sending batch of tweets from timeline of $screenName: " + tweets.size())
          batchFeatureExtraction ! TweetBatch(tweets.toList)
        }
        // Get the metadata we stored on the user
        (userMetadataReader ? UserMetadataQuery(screenName)).mapTo[UserMetadata].map(meta => Ok(Json.obj(
          "metadata" -> Json.toJson(meta),
          "timeline" -> Json.toJson(tweets.toList)
        )))
    }


  }

  def getUserRelevance(screenName: String, query: String) = Action.async {
    (labelStore ? GetUserRelevance(screenName, query)).map {
      case userRel @ UserRelevance(_, _, _) => Ok(Json.toJson(userRel))
      case NoUserRelevanceDataOnRecord => NoContent
    }
  }

}

