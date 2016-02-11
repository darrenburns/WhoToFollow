package learn.actors

import java.util

import akka.actor.{Actor, ActorRef}
import channels.actors.MetricsReporting
import channels.actors.MetricsReporting.CollectionStats
import com.google.inject.Inject
import com.google.inject.name.Named
import di.NamedActor
import learn.actors.TweetStreamActor.TweetBatch
import org.apache.commons.io.IOUtils
import org.terrier.indexing.TaggedDocument
import org.terrier.indexing.tokenisation.Tokeniser
import org.terrier.realtime.memory.MemoryIndex

import scala.collection.immutable.HashMap

object Indexer extends NamedActor {
  final val name = "Indexer"

  val index = new MemoryIndex()
  val tokeniser = Tokeniser.getTokeniser
  var docIds = new HashMap[String, Int]()  // TwitterUserId -> TerrierDocId
  var userCount = 0

  case object GetCollectionStats
}

/*
  Handles Terrier indexing of streaming tweets in real-time
 */
class Indexer @Inject() (
  @Named(MetricsReporting.name) metricsReporting: ActorRef
) extends Actor {

  import Indexer._

  override def receive = {
    case TweetBatch(batch) =>
      batch.foreach(status => {

        val tweet = if (status.isRetweet) status.getRetweetedStatus else status

        // Converting user Twitter IDs to integers
        val longUserNo = tweet.getUser.getId.toString

        // Build the TREC doc
        val trecStatus = s"<DOC><DOCNO>$longUserNo</DOCNO>${tweet.getText}</DOC>"
        val doc = new TaggedDocument(IOUtils.toInputStream(trecStatus, "UTF-8"),
          new util.HashMap[String, String](), tokeniser)

        docIds.get(longUserNo) match {
          case Some(docId) =>
            // Seen this user before, add the new tweet to their document
            index.addToDocument(docId, doc)
          case None =>
            // First time we've seen this user
            // Store the metadata in the metaindex
            val user = tweet.getUser
            doc.setProperty("username", user.getScreenName)
            doc.setProperty("name", user.getName)
            // Index user for the first time
            index.indexDocument(doc)
            val docId = index.getCollectionStatistics.getNumberOfDocuments - 1
            docIds += (longUserNo -> docId.toInt)
        }
      })

    case GetCollectionStats => metricsReporting ! getCollectionStats
  }

  def getCollectionStats = CollectionStats(index.getCollectionStatistics.getNumberOfDocuments)

}
