package actors

import java.util

import actors.TweetStreamActor.TweetBatch
import akka.actor.Actor
import org.apache.commons.io.IOUtils
import org.terrier.indexing.TaggedDocument
import org.terrier.indexing.tokenisation.Tokeniser
import org.terrier.realtime.memory.MemoryIndex
import play.api.Logger

import scala.collection.immutable.HashMap

object Indexer {
  val index = new MemoryIndex()
  val tokeniser = Tokeniser.getTokeniser
  var docIds = new HashMap[Int, Int]()
  var intIds = new HashMap[Long, Int]()
}

/*
  Handles Terrier indexing of streaming tweets in real-time
 */
class Indexer extends Actor {

  import Indexer._
  var userCount = 0

  override def receive = {
    case TweetBatch(batch) =>

      batch.foreach(status => {
        // Converting user Twitter IDs to integers
        val longId = status.getUser.getId
        val userId = intIds.get(longId) match {
          case Some(intId) =>  intId  // We've seen this user before
          case None =>
            userCount += 1
            intIds += (longId -> userCount)
            userCount
        }

        // Build the TREC doc
        val trecStatus = s"<DOC><DOCNO>$userId</DOCNO>${status.getText}</DOC>"
        val doc = new TaggedDocument(IOUtils.toInputStream(trecStatus, "UTF-8"),
          new util.HashMap[String, String](), tokeniser)

        docIds.get(userId) match {
          case Some(docId) =>
            Logger.debug(s"Adding to document. docId: $docId")
            index.addToDocument(docId, doc)
          case None =>
            index.indexDocument(doc)
            val docId = index.getMetaIndex.getItem("docno", userId)  // Throwing ArrayIndexOutOfBounds
            Logger.debug(s"Indexing document. Mapping userId:$userId -> docId:$docId")
            docIds += (userId -> docId)
        }

      })

    }

}
