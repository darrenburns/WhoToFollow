package channels.actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import akka.util.Timeout
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import com.google.inject.name.Named
import learn.actors.BatchFeatureExtraction
import org.joda.time.DateTime
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.{Configuration, Logger}
import query.actors.QueryService
import query.actors.QueryService.{Query, TerrierResultSet, UserTerrierScore}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}

object QueryWorker {

  trait Factory extends GenericChannel.Factory

  object Defaults {
    val Expiry = 15000
  }

  implicit val userTerrierScoreWrites = new Writes[UserTerrierScore] {
    def writes(uts: UserTerrierScore) = Json.obj(
      "screenName" -> uts.screenName, "name" -> uts.name, "score" -> uts.score
    )
  }

  implicit val terrierResultSetWrites = new Writes[TerrierResultSet] {
    def writes(trs: TerrierResultSet) = Json.obj(
      "totalResultSize" -> trs.actualSize,
      "results" -> Json.toJson(trs.userScores)
    )
  }

  case class QueryResults(query: String, userScores: Array[UserTerrierScore])
  case object FetchLatestQueryResults

}

class QueryWorker @Inject() (
  @Assisted queryString: String,
  @Assisted channel: Concurrent.Channel[JsValue],
  @Assisted parent: ActorRef,
  config: Configuration,
  @Named(BatchFeatureExtraction.name) batchFeatureExtraction: ActorRef,
  @Named(QueryService.name) queryService: ActorRef,
  @Named(QuerySupervisor.name) querySupervisor: ActorRef
) extends Actor with GenericChannel.Worker {

  import QueryWorker._

  Logger.debug(s"QueryWorker ${self.path} started.")

  implicit val timeout = Timeout(10, TimeUnit.SECONDS)
  override val channelName = queryString
  override val channelExpiry = config.getInt("channels.query.expiry").getOrElse(Defaults.Expiry)
  override var latestKeepAlive = DateTime.now()

  val fetchTick = context.system.scheduler
    .schedule(Duration.Zero, FiniteDuration(500, TimeUnit.MILLISECONDS), self, FetchLatestQueryResults)
  val expiryTick = context.system.scheduler
    .schedule(Duration.Zero, FiniteDuration(30, TimeUnit.SECONDS), self, CheckExpired)


  override def receive = LoggingReceive {
    case FetchLatestQueryResults => pushLatest()
    case trs @ TerrierResultSet(query, actualResultSize, results) => channel push Json.toJson(trs)
    case KeepAlive => handleKeepAlive()
    case CheckExpired => if (hasExpired) suicideAndCleanup(querySupervisor)
  }

  override def pushLatest(): Unit = {
    queryService ! Query(queryString)
  }

  override def postStop(): Unit = {
    fetchTick.cancel()
    expiryTick.cancel()
  }

}
