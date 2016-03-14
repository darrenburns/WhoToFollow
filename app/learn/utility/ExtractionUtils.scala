package learn.utility

import learn.actors.FeatureExtraction.TweetFeatures
import persist.actors.RedisWriterWorker.UserHashtagReport
import twitter4j.Status
import utils.QualityAnalyser


object ExtractionUtils {

  case class UserHashtagCount(username: String, hashtag: String, count: Int)

  def getStatusFeatures(status: Status): TweetFeatures = {
    val qa = new QualityAnalyser(status.getText)
    var s = status
    if (status.isRetweet) {
      s = status.getRetweetedStatus
    }
    val htCount = s.getHashtagEntities.length
    val mentionCount = s.getUserMentionEntities.length
    val linkCount = s.getURLEntities.length
    TweetFeatures(
      status = s,
      id = s.getId,
      username = s.getUser.getScreenName,
      followerCount = s.getUser.getFollowersCount,
      punctuationCounts = qa.findPunctuationCounts(),
      wordCount = qa.countWords() - htCount - mentionCount - linkCount,
      capWordCount = qa.countCapitalisedWords(),
      hashtagCount = htCount,
      retweetCount = s.getRetweetCount,
      mentionCount = mentionCount,
      likeCount = s.getFavoriteCount,
      dictionaryHits = qa.countDictionaryHits(),
      linkCount = linkCount
    )
  }

  def getHashtagCounts(status: Status): UserHashtagReport = {
    UserHashtagReport(status.getHashtagEntities.map(hashtag => {
        UserHashtagCount(status.getUser.getScreenName, hashtag.getText, 1)
    }))
  }

}