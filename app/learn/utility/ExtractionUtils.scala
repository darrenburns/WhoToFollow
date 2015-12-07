package learn.utility

import learn.actors.FeatureExtraction.TweetFeatures
import twitter4j.Status
import utils.QualityAnalyser

object ExtractionUtils {

  def getStatusFeatures(status: Status): TweetFeatures = {
    val qa = new QualityAnalyser(status.getText)
    val htCount = status.getHashtagEntities.length
    val mentionCount = status.getUserMentionEntities.length
    val linkCount = status.getURLEntities.length
    TweetFeatures(
      username = status.getUser.getScreenName,
      followerCount = status.getUser.getFollowersCount,
      punctuationCounts = qa.findPunctuationCounts(),
      wordCount = qa.countWords() - htCount - mentionCount - linkCount,
      capWordCount = qa.countCapitalisedWords(),
      hashtagCount = htCount,
      retweetCount = status.getRetweetCount,
      mentionCount = mentionCount,
      likeCount = status.getFavoriteCount,
      dictionaryHits = qa.countDictionaryHits(),
      linkCount = linkCount
    )
  }

}