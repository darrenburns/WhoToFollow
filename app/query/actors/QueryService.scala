package query.actors

import akka.actor.{Actor, ActorRef}
import channels.actors.QuerySupervisor
import com.google.inject.Inject
import com.google.inject.name.Named
import di.NamedActor
import learn.actors.BatchFeatureExtraction.FetchAndAnalyseTimeline
import learn.actors.{BatchFeatureExtraction, Indexer}
import org.terrier.matching.ResultSet
import org.terrier.querying.Manager
import play.api.{Configuration, Logger}


object QueryService extends NamedActor {
  override final val name = "QueryService"

  case class TerrierResultSet(originalQuery: String, actualSize: Int, userScores: Array[UserTerrierScore])
  case class Query(query: String)
  case class UserTerrierScore(screenName: String, name: String, query: String, score: Double)
}

class QueryService @Inject()
(
  @Named(QuerySupervisor.name) querySupervisor: ActorRef,
  @Named(BatchFeatureExtraction.name) batchFeatureExtraction: ActorRef,
  config: Configuration
) extends Actor {

  import QueryService._

  val resultSetSize = config.getInt("results.resultSize").getOrElse(20)

  val memIndex = Indexer.index

  override def receive = {
    case Query(queryString) =>
      Logger.debug(s"QueryService has received query: '$queryString'.")
      querySupervisor ! doQuery(queryString)
  }

  /* We can't reply to the `sender` here because it is unstable. Each time a QueryWorker asks QueryService
  for the latest TerrierResultSet, the `sender` ref in any executing actor is altered. Therefore we have to
  send our response to the QuerySupervisor who can then forward it to the relevant worker actor.
  See also: "The Cameo design pattern" and "anonymous actors".
  */

  def doQuery(queryString: String): TerrierResultSet = {

    if (queryString.isEmpty) {
      TerrierResultSet(queryString, 0, new Array[UserTerrierScore](0))
    }

    // create a search manager (runs the search process over an index)
    val queryingManager = new Manager(memIndex)

    // a search request represents the search to be carried out
    val srq = queryingManager.newSearchRequest("query", queryString)
    srq.setOriginalQuery(queryString)

    srq.addMatchingModel("Matching", "BM25")

    // Run the four stages of a Terrier search
    queryingManager.runPreProcessing(srq)
    queryingManager.runMatching(srq)
    queryingManager.runPostProcessing(srq)
    queryingManager.runPostFilters(srq)

    // Send the result set to the channel manager who will forward it through the socket to connected clients.
    val results = srq.getResultSet
    val actualResultSize = results.getResultSize

    Logger.debug(s"QueryService has obtained initial ResultSet from Terrier: $actualResultSize result(s).")

    results.setExactResultSize(resultSetSize)
    results.setResultSize(resultSetSize)

    val docIds = results.getDocids
    val metaIndex = Indexer.index.getMetaIndex

    // Get a list of usernames and screennames from the docIds
    val profiles = docIds.slice(0, resultSetSize).map(docId => {
      // Get the username metadata for the current docId
      val usernameOption = Option(metaIndex.getItem("username", docId))
      val nameOption = Option(metaIndex.getItem("name", docId))
      (usernameOption, nameOption) match {
        case (Some(username), Some(screenName)) =>
          batchFeatureExtraction ! FetchAndAnalyseTimeline(username)
          (username, screenName)
        case (None, None) =>
          Logger.error("USERNAME metadata not found in document.")
          (docId.toString, docId.toString)
      }
    })

    // Get the sequence of user -> score
    val scores = results.getScores
    val queryResults = (profiles zip scores) map {
      case ((screenName: String, name: String), score: Double) =>
        UserTerrierScore(screenName, name, queryString, score)
    }

    TerrierResultSet(queryString, actualResultSize, queryResults)
  }

}

