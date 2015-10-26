package init

import javax.inject.{Inject, Singleton}
import play.api.inject.ApplicationLifecycle
import scala.concurrent.Future

@Singleton
class ApplicationShutdown @Inject() (lifeCycle: ApplicationLifecycle) {

    lifeCycle.addStopHook { () =>
      Future.successful(SparkInit.ssc.stop(true))
    }

}
