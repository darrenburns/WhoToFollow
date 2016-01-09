package modules

import com.google.inject.AbstractModule
import hooks.{ApplicationShutdown, ApplicationPreStart}


class AppModule extends AbstractModule {
  def configure() = {
    bind(classOf[ApplicationPreStart]).asEagerSingleton()
    bind(classOf[ApplicationShutdown]).asEagerSingleton()
  }
}