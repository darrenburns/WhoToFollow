package learn.actors

import java.util

import akka.actor.{ActorRef, Actor}
import com.google.inject.Inject
import com.google.inject.name.Named
import learn.actors.TweetStreamActor.TweetBatch
import org.apache.commons.io.IOUtils
import org.terrier.indexing.TaggedDocument
import org.terrier.indexing.tokenisation.Tokeniser
import org.terrier.realtime.memory.MemoryIndex
import persist.actors.RedisWriter.IncrementIndexSize
import play.api.Logger

import scala.collection.immutable.HashMap

object Indexer {
  val index = new MemoryIndex()
  val tokeniser = Tokeniser.getTokeniser
  var docIds = new HashMap[String, Int]()
  var userCount = 0
}

/*
  Handles Terrier indexing of streaming tweets in real-time
 */
class Indexer @Inject()
(
  @Named("redisWriter") redisWriter: ActorRef
) extends Actor {

  import Indexer._

  override def receive = {
    case TweetBatch(batch) =>
      // Increment the index size in Redis by the size of the batch
      // TODO: Just change the value to the size of the index whenever indexing is working
      redisWriter ! IncrementIndexSize(batch.size)


      batch.foreach(status => {
        // Converting user Twitter IDs to integers
        val longUserNo = status.getUser.getId.toString


        // Build the TREC doc
        val trecStatus = s"<DOC><DOCNO>$longUserNo</DOCNO>${status.getText}</DOC>"
        val doc = new TaggedDocument(IOUtils.toInputStream(trecStatus, "UTF-8"),
          new util.HashMap[String, String](), tokeniser)

        docIds.get(longUserNo) match {
          case Some(docId) =>
            index.addToDocument(docId, doc)
          case None =>
            index.indexDocument(doc)
            val docId = index.getCollectionStatistics.getNumberOfDocuments -1
            docIds += (longUserNo -> docId.toInt)
        }
      })

    }

}
