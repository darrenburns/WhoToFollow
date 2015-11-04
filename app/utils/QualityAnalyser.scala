package utils

import play.api.Play.current


object QualityAnalyser {
  val dictionaryPath = current.configuration.getString("analysis.featureExtraction.dictionary")
    .getOrElse("/usr/share/dict/american-english")
  val dictionary = scala.io.Source.fromFile(dictionaryPath).getLines.toSet
}

/**
  * Defines a set of functions for analysing the quality of text.
  */
class QualityAnalyser(text: String) extends Serializable {

  import QualityAnalyser._
  private val splitText = text.split(" ")

  /** Count the numbers of punctuation characters in a string.
   *
   * @return A Map of the basic punctuation characters to the number of times they appear in the input string.
   *         If the punctuation character is not included in the result then it is not contained
   *         within the text.
   */
  def findPunctuationCounts(): Map[Char, Int] = {
    var counts = Map[Char, Int]().withDefaultValue(0)
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

  def countWords(): Int = {
    splitText.length
  }

  /** Count the number of capitalised words in a string.
   *
   * @return The number of words written entirely in capital letters within the input string.
   */
  def countCapitalisedWords(): Int = {
    var count = 0
    splitText.foreach(word => {
      val uppers = word.filter(_.isUpper)
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
    splitText.foreach(word =>
      if (dictionary.contains(word)) {
        hits += 1
      }
    )
    hits
  }

}
