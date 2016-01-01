package query.actors

import akka.actor.Actor
import learn.actors.Indexer
import org.terrier.matching.ResultSet
import org.terrier.querying.Manager

object QueryService {

  /* Sendables */
  case class QueryResults(originalQuery: String, resultSet: ResultSet)

  /* Receivables */
  case class Query(query: String)

}

class QueryService extends Actor {

  import QueryService._

  override def receive = {
    case Query(queryString) =>
      // Retrieve the memory index.
      val memIndex = Indexer.index

      // create a search manager (runs the search process over an index)
      val queryingManager = new Manager(memIndex)

      // a search request represents the search to be carried out
      val srq = queryingManager.newSearchRequest("query", queryString)
      srq.setOriginalQuery(queryString)

      // define a matching model, in this case use the classical BM25 retrieval model
      srq.addMatchingModel("Matching","BM25")

      // Run the four stages of a Terrier search
      queryingManager.runPreProcessing(srq)
      queryingManager.runMatching(srq)
      queryingManager.runPostProcessing(srq)
      queryingManager.runPostFilters(srq)

      // Send the result set to the channel manager who will forward it through the socket to connected clients.
      val results = srq.getResultSet
      sender ! QueryResults(queryString, results)
  }

}
