package report.actors

import java.nio.channels.ClosedChannelException
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import com.github.nscala_time.time.Imports._
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.joda.time.DateTime
import persist.actors.RedisReader.{ExpertRating, UserFeatures}
import persist.actors.RedisWriter.NewQuery
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import report.actors.ChannelManager.UserTerrierScore
import report.actors.MetricsReporting.RecentQueries
import report.utility.{ChannelUtilities, KeepAlive}

import scala.collection.immutable.HashMap
import scala.concurrent.duration.{Duration, FiniteDuration}

object WebSocketSupervisor {

  object Defaults {
    val RecentQueriesChannelName = "default:recent-queries"
    val LatestIndexSizeChannelName = "default:index-size"
    val DeadChannelTimeout = 30
  }

  implicit val queryResultsWrites = new Writes[QueryResults] {
    def writes(results: QueryResults) = {
      val scoreJsonList = results.userScores.map(result => {
          Json.obj("screenName" -> result.screenName, "score" -> result.score)
      })
      Json.obj("query" -> results.query, "results" -> Json.toJson(scoreJsonList))
    }
  }

  implicit val expertRatingWrites = new Writes[ExpertRating] {
    def writes(rating: ExpertRating) = Json.obj(
      "query" -> rating.query,
      "username" -> rating.username,
      "rating" -> rating.rating
    )
  }

  implicit val recentQueriesWrites = new Writes[RecentQueries] {
    def writes(recentQueries: RecentQueries) = Json.obj(
      "recentQueries" -> recentQueries.recentQueriesList
    )
  }

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

  case class OutputChannel(name: String)
  case class CheckForDeadChannels()
  case class ChannelTriple(in: Iteratee[JsObject, Unit], out: Enumerator[JsValue],
                          channel: Concurrent.Channel[JsValue])
  case class CollectionStats(numberOfDocuments: Int)
  case class QueryResults(query: String, userScores: Array[UserTerrierScore])
}


@Singleton
class WebSocketSupervisor @Inject()
(
  channelManagerFactory: ChannelManager.Factory,
  @Named("redisWriter") redisWriter: ActorRef
) extends Actor with InjectedActorSupport {

  import WebSocketSupervisor._

  protected[this] var channels: HashMap[String, ChannelTriple] = HashMap.empty
  protected[this] var channelManagers: HashMap[String, ActorRef] = HashMap.empty
  protected[this] var keepAlives: HashMap[String, DateTime] = HashMap.empty

  // Create the default channels
  createChannel(Defaults.RecentQueriesChannelName)
  createChannel(Defaults.LatestIndexSizeChannelName)

  implicit val timeout = Timeout(20, TimeUnit.SECONDS)

  context.system.scheduler.schedule(Duration.Zero, FiniteDuration(20, TimeUnit.SECONDS), self, CheckForDeadChannels())

  override def receive = {

    /*
     Request for an output channel fetch/creation depending on whether it is already
     active or not. Triggered when the client makes a new query and thus requests
     the creation of a new WebSocket.
     */
    case OutputChannel(query) =>
      channels.get(query) match {
        case Some(ch) =>
          Logger.debug(s"Fetching existing channel: $query")
          sender ! Right((ch.in, ch.out))
        case None =>
          val lowerChannel = query.toLowerCase
          val ch = if (ChannelUtilities.isQueryChannel(query)) {
            Logger.debug(s"Creating query channel for query: $query")
            redisWriter ! NewQuery(lowerChannel)
            createChannel(lowerChannel)
          } else if (ChannelUtilities.isUserAnalysisChannel(query)) {
            createChannel(query)
          }
          sender ! (ch match {
            case chTriple: ChannelTriple => Right((chTriple.in, chTriple.out))
            case _ => Left("Not found")
          })
      }

    /*
     The expertise rankings retrieved for a given query. Results in the rankings/leaderboard
     being sent through the channel associated with that query.
     */
    case results @ QueryResults(query, resultSet) =>
      val json = Json.toJson(results)
      Logger.debug("Outputting json results: " + json)
      val channelTriple = channels.get(query)
      channelTriple match {
        case Some(triple) =>
          try {
            triple.channel push json
          } catch {
            case cce: ClosedChannelException =>
              Logger.error(s"A client closed the connection to channel $query.")
            case e: Exception =>
              Logger.error(s"An exception occurred while pushing results to channel $query", e)
          }
        case None => Logger.error("Trying to send leaderboard through non-existent channel.")
      }

    /*
     Self-sent message. This actor messages itself repeatedly, requesting that any channels
     which haven't received a Keep-Alive within a set timeout range have their resources freed.
     */
    case CheckForDeadChannels() =>
      keepAlives.foreach(channel => {
        val channelName = channel._1
        val lastKeepAlive = channel._2
        if ((lastKeepAlive to DateTime.now).millis > Defaults.DeadChannelTimeout*1000) {
          // No Keep-Alives received within the specified time frame, so free resources
          Logger.info(s"Closing channel $channelName.")
          channels.get(channelName) match {
            case Some(chTriple) =>
              chTriple.channel.eofAndEnd()
              channels -= channelName
            case None => Logger.error("Trying to close a non-existent channel.")
          }
          channelManagers.get(channelName) match {
            case Some(handler) =>
              context stop handler
              channelManagers -= channelName
            case None => Logger.error(s"Tried to stop a non-existent QueryHandler for query $channelName")
          }
          keepAlives -= channelName
        }
      })

    /*
     The list of recent queries to display on the homepage of the UI
     */
    case msg @ RecentQueries(queries) =>
      val primaryChannelTriple = channels.get(Defaults.RecentQueriesChannelName)
      primaryChannelTriple match {
        case Some(chTriple) =>
          val json = Json.toJson(msg)
          try {
            chTriple.channel push json
          } catch {
            case cce: ClosedChannelException =>
              Logger.error(s"A client closed the connection to the recent queries channel.")
            case e: Exception =>
              Logger.error(s"An exception occurred while pushing results to recent queries channel.", e)
          }

        case None => Logger.error(s"Channel ${Defaults.RecentQueriesChannelName} does not exist.")
      }

    /*
    The most recent index size recorded in Redis.
     */
    case CollectionStats(numDocs) =>
      val indexSizeChannelTriple = channels.get(Defaults.LatestIndexSizeChannelName)
      indexSizeChannelTriple match {
        case Some(chTriple) =>
          val json = Json.obj("indexSize" -> numDocs)
          try {
            chTriple.channel push json
          } catch {
            case cce: ClosedChannelException =>
              Logger.error("The latest index size channel is closed.")
            case e: Exception =>
              Logger.error("An exception occurred while trying to push the most recent index size.", e)
        }
        case None => Logger.error(s"Channel ${Defaults.LatestIndexSizeChannelName} does not exist.")

      }

    case features @ UserFeatures(s,_,_,_,_,_,_,_,_,_) =>
      val chName = ChannelUtilities.getChannelNameFromScreenName(s)
      val userChannelTri = channels.get(chName)
      userChannelTri match {
        case Some(tri) =>
          Logger.debug("Features: " + features)
          try {
            tri.channel push Json.toJson(features)
          } catch {
            case cce: ClosedChannelException =>
              Logger.error(s"The user features channel for '$chName' was closed.")
            case e: Exception =>
              Logger.error(s"An exception occurred while trying to push the most recent" +
                s" features for channel: $chName", e)
          }
        case None => Logger.error(s"Unable to send user features through channel $chName")
      }

  }

  /**
    * Creates a new open channel for the given input query. The results for that query will be sent through
    * the open channel every time MetricsReporting asks for it.
    * @param query The name of the new channel (corresponds to the query text).
    * @return
    */
  private def createChannel(query: String) = {
    val (out, channel) = Concurrent.broadcast[JsValue]

    // Listen for Keep-Alives
    val in = Iteratee.foreach[JsObject] {q =>
      (q \ "request").as[String] match {
        case KeepAlive.messageType => sendKeepAlive((q \ "channel").as[String])
      }
    }

    // Construct and keep a reference to the channel components
    val chTriple = ChannelTriple(in, out, channel)
    channels += (query -> chTriple)

    // Store the manager and instantiate the keep-alive
    if (ChannelUtilities.isQueryChannel(query) || ChannelUtilities.isUserAnalysisChannel(query)) {
      channelManagers += (query -> injectedChild(channelManagerFactory(query), query))
      keepAlives += (query -> DateTime.now)
    }
    chTriple
  }

  /**
    In order to keep track of which WebSockets are still needed,
   the client repeatedly sends keep-alive messages directed at
   the channel they are interested in. The client does this every
   typescript/util/config.keepAliveFrequency seconds. Also every Defaults.DeadChannelTimeout
   seconds, we check the most recent keep-alive time. If a keep-alive hasn't
   been sent in the past minute for a given channel, then all resources for
   that channel are closed
  */
  private def sendKeepAlive(channelName: String): Unit = {
    Logger.info(s"Received keep-alive directed to channel $channelName")
    keepAlives += (channelName -> DateTime.now())
  }

}
