import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestActorRef}
import com.google.inject.AbstractModule
import di.{AppModule, ActorModule}
import hooks.SparkInit
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope
import play.api.Mode
import play.api.inject.{BindingKey, bind, Module}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.Akka
import play.api.test.{PlaySpecification, FakeApplication}
import query.actors.QueryService
import query.actors.QueryService.{UserTerrierScore, TerrierResultSet}

@RunWith(classOf[JUnitRunner])
class QueryServiceSpec extends PlaySpecification {

  sequential

  val EmptyQuery = ""

  val actorSystem = ActorSystem("test")
  class Actors extends TestKit(actorSystem) with Scope

  val app = new GuiceApplicationBuilder()
    .overrides(bind[ActorSystem].toInstance(actorSystem))
    .build()

  "A QueryService actor" should {

    "return an empty TerrierResultSet with an actual size of 0 on an empty query" in new Actors {
      running(app) {
        val queryServiceActor = TestActorRef(app.injector.instanceOf(BindingKey(classOf[QueryService]))).underlyingActor
        queryServiceActor.doQuery(EmptyQuery).actualSize must be equalTo 0
      }
    }

    "return a TerrierResultSet with no user score elements on an empty query" in new Actors {
      running(app) {
        val queryServiceActor = TestActorRef(app.injector.instanceOf(BindingKey(classOf[QueryService]))).underlyingActor
        queryServiceActor.doQuery(EmptyQuery).userScores.length must be equalTo 0
      }
    }

    "specify that the original query was empty on an empty query" in new Actors {
      running(app) {
        val queryServiceActor = TestActorRef(app.injector.instanceOf(BindingKey(classOf[QueryService]))).underlyingActor
        queryServiceActor.doQuery(EmptyQuery).originalQuery must be equalTo EmptyQuery
      }
    }

  }

}
