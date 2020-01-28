package controllers

import javax.inject.Inject

import securesocial.controllers.BaseLoginPage
import play.api.mvc.{ Action, AnyContent, RequestHeader }
import play.api.{ Configuration, Environment, Logging }
import play.filters.csrf.CSRFAddToken
import securesocial.core.RuntimeEnvironment
import securesocial.core.services.RoutesService

class CustomLoginController @Inject() (val csrfAddToken: CSRFAddToken, implicit override val env: RuntimeEnvironment)
  extends BaseLoginPage with Logging {
  override def login: Action[AnyContent] = {
    logger.debug("using CustomLoginController")
    super.login
  }
}

class CustomRoutesService(environment: Environment, configuration: Configuration)
  extends RoutesService.Default(environment, configuration) {
  override def loginPageUrl(implicit req: RequestHeader): String =
    absoluteUrl(controllers.routes.CustomLoginController.login())
}