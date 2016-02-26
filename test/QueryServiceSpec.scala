import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.BeforeAfterAll
import play.api.libs.concurrent.Akka
import play.api.test.FakeApplication
import query.actors.QueryService.{UserTerrierScore, TerrierResultSet}

@RunWith(classOf[JUnitRunner])
class QueryServiceSpec extends Specification with BeforeAfterAll {

  var fakeApp: FakeApplication = null
  implicit var system: ActorSystem = null
  override def beforeAll(): Unit = {
    fakeApp = FakeApplication()
    system = Akka.system(fakeApp)
  }

  override def afterAll(): Unit = {
    fakeApp.stop()
  }

  "A QueryService actor" should {
    "return an empty TerrierResultSet on an empty query" in {
      val queryServiceActorRef = TestActorRef()
      val actor = queryServiceActorRef.underlyingActor
      val query = ""
      actor.doQuery(query) should be(TerrierResultSet(query, new Array[UserTerrierScore](0)))
    }

  }

}
