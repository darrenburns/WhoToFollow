package utils

import learn.actors.FeatureExtraction.TweetFeatures
import play.api.Play.current
import twitter4j.Status


object QualityAnalyser {
  lazy val dictionaryPath = current.configuration.getString("analysis.featureExtraction.dictionary")
    .getOrElse("/usr/share/dict/words")
  lazy val dictionary = scala.io.Source.fromFile(dictionaryPath).getLines.toSet

  def isStatusHighQuality(features: TweetFeatures): Boolean = {
//    (features.wordCount > 0 && features.dictionaryHits/features.wordCount > 0.5) &&
      (features.wordCount > 3)
//      (features.followerCount >= 300)
//      (features.capWordCount < features.wordCount) &&
//      (features.hashtagCount < 5)
  }

}

/**
  * Defines a set of functions for analysing the quality of text.
  */

class QualityAnalyser(text: String) extends Serializable {

  import QualityAnalyser._
  private val splitText = text.split(" ")
  private val words = splitText.map(_.filter(!StringUtilities.BasicPunctuation.contains(_)))

  /** Count the numbers of punctuation characters in a string.
   *
   * @return A Map of the basic punctuation characters to the number of times they appear in the input string.
   *         If the punctuation character is not included in the result then it is not contained
   *         within the text.
   */
  def findPunctuationCounts(): Map[Char, Int] = {
    var counts = Map[Char, Int]().empty
    text.foreach(c => {
      if (StringUtilities.BasicPunctuation.contains(c)) {
        counts.get(c) match {
          case Some(count) => counts += (c -> (count + 1))
          case None => counts += (c -> 1)
        }
      }
    })
    counts
  }

  def countWords(): Int = splitText.length

  /** Count the number of capitalised words in a string.
   *
   * @return The number of words written entirely in capital letters within the input string.
   */
  def countCapitalisedWords(): Int = {
    var count = 0
    words.foreach(word => {
      val uppers = word.filter(c => c.isUpper)
      if (uppers.length == word.length) {
        count += 1
      }
    })
    count
  }

  /** Count the number of words in the text that are found upon looking up the dictionary specified
    * in application.conf.
    *
    * @return The number of words in the text that are contained within the dictionary.
    */
  def countDictionaryHits(): Int = {
    var hits = 0
    words.foreach(word => {
      if (!word.isEmpty && !word.startsWith("#") && !word.startsWith("@") && dictionary.contains(word.toLowerCase)) {
        hits += 1
      }
    })
    hits
  }

}
