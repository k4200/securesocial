package controllers

import javax.inject.Inject

import play.filters.csrf.CSRFAddToken
import securesocial.controllers.BaseLoginPage
import play.api.mvc.{ Action, AnyContent, RequestHeader }
import play.api.{ Environment, Configuration, Logger }
import play.filters.csrf.CSRFAddToken
import securesocial.core.{ IdentityProvider, RuntimeEnvironment }
import securesocial.core.services.RoutesService

class CustomLoginController @Inject() (val csrfAddToken: CSRFAddToken, implicit override val env: RuntimeEnvironment) extends BaseLoginPage {
  override def login: Action[AnyContent] = {
    Logger.debug("using CustomLoginController")
    super.login
  }
}

class CustomRoutesService(configuration: Configuration, playEnv: Environment) extends RoutesService.Default(configuration, playEnv) {
  override def loginPageUrl(implicit req: RequestHeader): String = controllers.routes.CustomLoginController.login().absoluteURL(sslEnabled.value)
}