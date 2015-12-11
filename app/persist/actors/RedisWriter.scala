package persist.actors

import akka.actor.Actor
import com.google.inject.Singleton
import hooks.RedisConnectionPool
import learn.actors.FeatureExtraction.TweetFeatures
import learn.actors.UserHashtagCounter.{UserHashtagCount, UserHashtagReport}
import persist.actors.RedisWriter._
import play.api.Logger
import twitter4j.Status


/*
 Reports can be sent here for storage in Redis.
 */
object RedisWriter {
  case class NewQuery(query: String)
  case class IncrementIndexSize(incrementAmount: Int)
  case class HashtagCountUpdate(results: Seq[UserHashtagCount])
  case class TweetFeatureBatch(reports: Seq[TweetFeatures])
  case class ProcessedTweets(tweets: Seq[twitter4j.Status])

  /**
    * @param tweets A sequence of (username, tweetId) tuples. This is all the information
    *               required to tag the tweet as processed.
    */
  case class ProcessedTweetTuples(tweets: Seq[(String, Long)])
}

@Singleton
class RedisWriter extends Actor with Serializable {

  private val clients = RedisConnectionPool.pool

  override def receive = {
    case UserHashtagReport(results) =>
      applyHashtagCounts(results)
    case TweetFeatureBatch(reports) =>
      Logger.debug("Received latest tweet features.")
      updateExtractedFeatures(reports)
    case NewQuery(q) =>
      addQuery(q)
    case ProcessedTweets(tweets) =>
      markTweetsAsProcessed(tweets)
    case ProcessedTweetTuples(tweets) =>
      markTweetTuplesAsProcessed(tweets)
    case IncrementIndexSize(amount) =>
      incrementIndexSize(amount)
  }

  /**
    * Adds the query to the list of recent queries
    */
  def addQuery(query: String) = {
    clients.withClient{client =>
      client.lpush("recentQueries", query)
    }
  }

  /** Increments the hashtag counts stored in Redis to the latest correct value.
   *
   * @param hashtagCounts The sequence of UserHashtagCount objects to update Redis with.
   */
  def applyHashtagCounts(hashtagCounts: Seq[UserHashtagCount]): Unit = {
    clients.withClient{client =>
      hashtagCounts.foreach(userTagCount => {
        client.zincrby(s"hashtags:${userTagCount.hashtag}", userTagCount.count, userTagCount.username)
      })
    }
  }

  /** Updates user models based on the features extracted
   *
   * @param reports The sequence of TweetQualityReports to update Redis with.
   */
  def updateExtractedFeatures(reports: Seq[TweetFeatures]): Unit = {
    /*
    A report looks like this:

      case class TweetQualityReport(username: String, followerCount: Int, punctuationCounts: Map[Char, Int],
                                wordCount: Int, capWordCount: Int, hashtagCount: Int, retweetCount: Long,
                                mentionCount: Int, likeCount: Int, dictionaryHits: Int, linkCount: Int)

     */
    clients.withClient{client =>
      Logger.debug("Writing batch of feature reports to Redis.")
      reports.foreach(report => {
        val user = report.username
          // Increment the value at <username>:stats:tweetCount by 1
          client.hincrby(s"user:$user:stats", "tweetCount", 1)
          // Set the value at <username>:stats:followerCount to report.followerCount
          client.hset(s"user:$user:stats", "followerCount", report.followerCount)
          // Increment the values at <username>:stats:<char> by report.punctuationCounts
          report.punctuationCounts.foreach(count => {
            client.hincrby(s"user:$user:stats", s"${count._1}", count._2)
          })
          // Increment the value at <username>:stats:wordCount by report.wordCount
          client.hincrby(s"user:$user:stats", "wordCount", report.wordCount)
          // Increment the value at <username>:stats:capWordCount by report.capWordCount
          client.hincrby(s"user:$user:stats", "capWordCount", report.capWordCount)
          // Increment the value at <username>:stats:hashtagCount by report.hashtagCount
          client.hincrby(s"user:$user:stats", "hashtagCount", report.hashtagCount)
          // Increment the value at <username>:stats:retweetCount by report.retweetCount
          client.hincrby(s"user:$user:stats", "retweetCount", report.retweetCount)
          // Increment the value at <username>:stats:likeCount by report.likeCount
          client.hincrby(s"user:$user:stats", "likeCount", report.likeCount)
          // Increment the value at <username>:stats:dictionaryHits
          client.hincrby(s"user:$user:stats", "dictionaryHits", report.dictionaryHits)
          // Increment the link count
          client.hincrby(s"user:$user:stats", "linkCount", report.linkCount)
        })
    }
  }

  /**
    * Marks a sequence of tweets as processed in Redis to ensure we don't process it more than once.
    *
    * @param tweets A sequence of tweets
    */
  def markTweetsAsProcessed(tweets: Seq[Status]): Unit = {
    clients.withClient{client =>
      tweets.foreach(tweet => {
        client.sadd(s"user:${tweet.getUser.getScreenName}:tweetIds", tweet.getId)
      })
    }
  }

  /**
    * Marks a sequence of (screenName, tweetId) tuples as processed in Redis.
    *
    * @param tweetIds A sequence of (screenName: String, tweetId: Long) tuples.
    */
  def markTweetTuplesAsProcessed(tweetIds: Seq[(String, Long)]): Unit = {
    clients.withClient{client =>
      tweetIds.foreach(t => {
        client.sadd(s"user:${t._1}:tweetIds", t._2)
      })
    }
  }

  /**
    * Increments the counter for the size of the index in Redis.
    *
    * @param amount The amount to increment the index size by.
    */
  def incrementIndexSize(amount: Int): Unit = {
    clients.withClient{client =>
      client.incrby("indexSize", amount)
    }
  }

}

