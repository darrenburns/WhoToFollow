import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestProbe, TestKit}
import hooks.SparkInit
import learn.actors.{TweetStreamSimulator, FeatureExtraction}
import learn.actors.TweetStreamActor.PipelineActorReady
import learn.actors.HashtagCounter.ActiveTwitterStream
import org.apache.spark.streaming.twitter.TwitterUtils
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope
import play.api.{Environment, Configuration}
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.PlaySpecification
import query.actors.QueryService
import twitter4j.Status

import scala.concurrent.duration.FiniteDuration


@RunWith(classOf[JUnitRunner])
class FeatureExtractionSpec extends PlaySpecification {

  sequential

  val SampleTweetText = List("Hello world!!!", "another tweet")

  val actorSystem = ActorSystem("test")
  class Actors extends TestKit(actorSystem) with Scope

  val app = new GuiceApplicationBuilder()
    .overrides(bind[ActorSystem].toInstance(actorSystem))
    .configure("stream.sourcefile.path" -> "/Users/darrenburns/Resources/brits1.gz")
    .build()

  "The FeatureExtraction actor should respond that it is ready when it receives a Twitter stream handle" in new Actors {
    running(app) {
      val featureExtractionActorRef = TestActorRef(app.injector.instanceOf(BindingKey(classOf[FeatureExtraction])))
      val probe = TestProbe()
      val config = app.injector.instanceOf(classOf[Configuration])
      val streamPath = app.configuration.getString("stream.sourcefile.path").get
      val handle = SparkInit.ssc.actorStream[Status](TweetStreamSimulator.props[Status](streamPath, 10, 10), TweetStreamSimulator.name)
      featureExtractionActorRef.tell(ActiveTwitterStream(handle), probe.ref)
      probe.expectMsgType[PipelineActorReady](FiniteDuration(10, TimeUnit.SECONDS))
    }
  }

}
