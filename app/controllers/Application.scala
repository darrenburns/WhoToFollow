package controllers

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.Inject
import com.google.inject.name.Named
import learn.actors.BatchFeatureExtraction
import learn.actors.TweetStreamActor.TweetBatch
import persist.actors.LabelStore.Vote
import play.api.Logger
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.{JsString, Json, Writes, JsValue}
import play.api.mvc._
import report.actors.WebSocketSupervisor.OutputChannel
import twitter4j.{ResponseList, Status, TwitterFactory}

import scala.concurrent.duration._
import scala.collection.JavaConversions._


object Application {

  implicit val statusWrites = new Writes[Status] {
    def writes(status: Status) = Json.obj(
      "id" -> status.getId,
      "text" -> status.getText,
      "username" -> status.getUser.getName,
      "screenname" -> status.getUser.getScreenName,
      "date" -> status.getCreatedAt,
      "retweets" -> status.getRetweetCount,
      "avatar" -> status.getUser.getProfileImageURL
    )
  }

  implicit val responseListWrites = new Writes[ResponseList[twitter4j.Status]] {
    def writes(responseList: ResponseList[Status]) = {
      val list = Vector.empty[Status]
      responseList.foreach(status => {
        list :+ status
      })
      Json.obj(
        "tweets" -> Json.toJson(list)
      )
    }

  }

}

class Application @Inject()
(
  @Named("webSocketSupervisor") webSocketSupervisor: ActorRef,
  @Named("labelStore") labelStore: ActorRef,
  @Named("batchFeatureExtraction") batchFeatureExtraction: ActorRef
) extends Controller {

  import Application._

  implicit val timeout = Timeout(5 seconds)

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
  def voteForUser = Action { request =>
    val json = request.body.asJson.get
    val vote = json.as[Vote]
    Logger.debug(s"Action: Received vote for ${vote.screenName} in" +
      s" topic '${vote.hashtag}': Vote value = ${vote.voteId}")
    labelStore ! vote
    Ok // TODO: Assuming success
  }

  /**
    * Receives a GET request which contains the Twitter screen name (e.g. @darren) of the use
    * whose timeline we are to return and also analyse.
    *
    * @return An HTTP response with a JSON object containing a list of the tweets present in the timeline
    *         of the user contained within the request parameters.
    */
  def fetchAndAnalyseTimeline(screenName: String) = Action { request =>
    // Fetch a list of tweets from the users timeline
    val twitter = TwitterFactory.getSingleton
    val tweets = twitter.getUserTimeline(screenName)
    batchFeatureExtraction ! TweetBatch(tweets)
    Ok(Json.toJson(tweets))
  }

}

