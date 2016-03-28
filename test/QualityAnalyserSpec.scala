import org.specs2.Specification
import utils.QualityAnalyser

class QualityAnalyserSpec extends Specification {
  def is = s2"""
  This specification checks that quality analysis features are being extracted as expected

  The first tweet text should
    contain 0 words                       $e1
    contain 0 capitalised words           $e2
    contain no punctuation                $e4

  The second tweet text should
    contain 0 words                       $e5
    contain 0 capitalised words           $e6
    contain no punctuation                $e8

  The third tweet text should
    contain exactly 1 word                $e9
    contain 0 capitalised words           $e10
    contain no punctuation                $e12

  The fourth tweet text should
    contain 2 words                       $e13
    contain 1 capitalised words           $e14
    contain no punctuation                $e16

  The fifth tweet text should
    contain 1 word
    contain no capitalised words
    contain 1 punctuation mark
  """

  sequential

  val tweets = List[String]("", " ", "@hello", "hello WORLD", "hello-world", "We see 16 sunrises every day - but I've never seen one as beautiful as this. Good morning Earth!")

  val firstTweet = tweets.head
  val qa1 = new QualityAnalyser(firstTweet)
  def e1 = qa1.countWords() must beEqualTo(0)
  def e2 = qa1.countCapitalisedWords() must beEqualTo(0)
  def e3 = qa1.countDictionaryHits() must beEqualTo(0)
  def e4 = qa1.findPunctuationCounts().size must beEqualTo(0)

  val secondTweet = tweets(1)
  val qa2 = new QualityAnalyser(secondTweet)
  def e5 = qa2.countWords() must beEqualTo(0)
  def e6 = qa2.countCapitalisedWords() must beEqualTo(0)
  def e8 = qa2.findPunctuationCounts().size must beEqualTo(0)

  val thirdTweet = tweets(2)
  val qa3 = new QualityAnalyser(thirdTweet)
  def e9 = qa3.countWords() must beEqualTo(1)
  def e10 = qa3.countCapitalisedWords() must beEqualTo(0)
  def e12 = qa3.findPunctuationCounts().size must beEqualTo(0)

  val fourthTweet = tweets(3)
  val qa4 = new QualityAnalyser(fourthTweet)
  def e13 = qa4.countWords() must beEqualTo(2)
  def e14 = qa4.countCapitalisedWords() must beEqualTo(1)
  def e16 = qa4.findPunctuationCounts().size must beEqualTo(0)

//  val fifthTweet = tweets(4)
//  val qa5 = new QualityAnalyser(fifthTweet)
//  def e17 = qa4.countWords() must beEqualTo(1)
//  def e18 = qa4.countCapitalisedWords() must beEqualTo(0)
//  def e19 = qa4.findPunctuationCounts().size must beEqualTo(0)

}
