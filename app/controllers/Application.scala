package controllers

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.github.nscala_time.time.Imports._
import com.google.inject.Inject
import com.google.inject.name.Named
import hooks.Twitter
import learn.actors.TweetStreamActor.TweetBatch
import persist.actors.LabelStore.Vote
import persist.actors.UserMetadataReader.UserMetadata
import persist.actors.UserMetadataWriter.UserMetadataQuery
import play.api.Logger
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc._
import report.actors.WebSocketSupervisor.OutputChannel
import twitter4j.Status

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

}

class Application @Inject()
(
  @Named("webSocketSupervisor") webSocketSupervisor: ActorRef,
  @Named("labelStore") labelStore: ActorRef,
  @Named("batchFeatureExtraction") batchFeatureExtraction: ActorRef,
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
    * a new channel named after the query. We can also create a channel which can be used to
    * update user features in realtime, or for updating the number of indexed documents on the
    * homepage.
    */
  def getChannel(name: String) = WebSocket.tryAccept[JsValue] { request =>
      // Ask the WebSocketSupervisor for the requested channel.
      // It will create it if it doesn't already exist.
      val future = (webSocketSupervisor ? OutputChannel(name))
                      .mapTo[Either[Result, (Iteratee[JsValue, _], Enumerator[JsValue])]]
      future
    }

  /**
    * Receives a POST request containing a Vote object which
    * is sent to the store for future machine learning tasks.
    * @return An HTTP 200 response
    */
  def rateUser = Action(BodyParsers.parse.json) { request =>
    val json = request.body.validate[Vote]
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
    * Receives a GET request which contains the Twitter screen name (e.g. @darren) of the use
    * whose timeline we are to return and also analyse.
    *
    * @return An HTTP response with a JSON object containing a list of the tweets present in the timeline
    *         of the user contained within the request parameters.
    */
  def fetchAndAnalyseTimeline(screenName: String) = Action.async { request =>

    // Fetch a list of tweets from the users timeline
    val twitter = Twitter.instance
    val tweets = twitter.getUserTimeline(screenName)
    if (tweets.nonEmpty) {
      Logger.debug(s"Sending batch of tweets from timeline of $screenName: " + tweets.size())
      batchFeatureExtraction ! TweetBatch(tweets.toList)
    }
    // Get the metadata we stored on the user
    val userMetaFuture = userMetadataReader ? UserMetadataQuery(screenName)
    userMetaFuture.map(meta => Ok(Json.obj(
      "metadata" -> Json.toJson(meta.asInstanceOf[UserMetadata]),
      "timeline" -> Json.toJson(tweets.toList)
    )))

  }

}

