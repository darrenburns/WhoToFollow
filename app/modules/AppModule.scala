package modules

import com.google.inject.AbstractModule
import init.{ApplicationShutdown, ApplicationStartup}


class AppModule extends AbstractModule {
  def configure() = {
    bind(classOf[ApplicationStartup]).asEagerSingleton()
    bind(classOf[ApplicationShutdown]).asEagerSingleton()
  }
}