package hooks

import javax.inject.{Inject, Singleton}
import play.api.inject.ApplicationLifecycle
import scala.concurrent.Future

/**
  * Contains hooks which are ran before the application shuts down.
  * This can be used to clean up resources such as the Spark streaming context.
  *
  * @param lifeCycle Pre-injected ApplicationLifestyle object provided to add hooks
  */
@Singleton
class ApplicationShutdown @Inject() (lifeCycle: ApplicationLifecycle) {

    lifeCycle.addStopHook { () =>
      Future.successful(SparkInit.ssc.stop(true))
    }

}
