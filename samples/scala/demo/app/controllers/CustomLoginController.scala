package controllers

import javax.inject.Inject

import play.filters.csrf.CSRFAddToken
import securesocial.controllers.BaseLoginPage
import play.api.mvc.{ RequestHeader, AnyContent, Action }
import play.api.{ Environment, Configuration, Logger }
import securesocial.core.{ RuntimeEnvironment, IdentityProvider }
import service.DemoUser
import securesocial.core.services.RoutesService

class CustomLoginController @Inject() (implicit val env: RuntimeEnvironment, val configuration: Configuration, val playEnv: Environment, val CSRFAddToken: CSRFAddToken) extends BaseLoginPage {
  override def login: Action[AnyContent] = {
    Logger.debug("using CustomLoginController")
    super.login
  }
}

class CustomRoutesService(implicit override val configuration: Configuration, override val playEnv: Environment) extends RoutesService.Default {
  override def loginPageUrl(implicit req: RequestHeader): String = controllers.routes.CustomLoginController.login().absoluteURL(sslEnabled.value)
}