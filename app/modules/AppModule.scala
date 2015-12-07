package modules

import com.google.inject.AbstractModule
import hooks.{ApplicationShutdown, ApplicationStartup}


class AppModule extends AbstractModule {
  def configure() = {
    bind(classOf[ApplicationStartup]).asEagerSingleton()
    bind(classOf[ApplicationShutdown]).asEagerSingleton()
  }
}