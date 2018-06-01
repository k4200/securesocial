/**
 * Copyright 2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import akka.actor.ActorSystem
import controllers.CustomRoutesService
import play.api.i18n.{ MessagesApi, Messages }
import play.api.libs.mailer.MailerClient
import play.api.libs.oauth.ServiceInfo
import play.api.libs.ws.WSClient
import play.api.mvc.RequestHeader
import play.api.{ Configuration, Environment }
import play.api.cache.CacheApi
import securesocial.core.providers.UsernamePasswordProviderConfigurations
import securesocial.core.{ ServiceInfoHelper, RuntimeEnvironment }
import securesocial.core.authenticator.{ HttpHeaderAuthenticatorConfigurations, CookieAuthenticatorConfigurations }
import service.{ DemoUser, InMemoryUserService, MyEventListener }

/**
 * The runtime environment for this sample app.
 */
class MyRuntimeEnvironment(val configuration: Configuration, val playEnv: Environment, val cacheApi: CacheApi, val messagesApi: MessagesApi, val WS: WSClient, val mailerClient: MailerClient, val actorSystem: ActorSystem) extends RuntimeEnvironment.Default {
  type U = DemoUser
  override implicit val executionContext = play.api.libs.concurrent.Execution.defaultContext
  override lazy val routes = new CustomRoutesService(configuration, playEnv)
  override lazy val userService: InMemoryUserService = new InMemoryUserService()
  override lazy val eventListeners = List(new MyEventListener())
  override val cookieAuthenticatorConfigurations = new CookieAuthenticatorConfigurations.Default(configuration, playEnv)
  override val httpHeaderAuthenticatorConfigurations = new HttpHeaderAuthenticatorConfigurations.Default(configuration, playEnv)
  val serviceInfoHelper = new ServiceInfoHelper.Default
  val usernamePasswordProviderConfigurations = new UsernamePasswordProviderConfigurations.Default(configuration)
}

/**
 * An implementation that checks if the controller expects a RuntimeEnvironment and
 * passes the instance to it if required.
 *
 * This can be replaced by any DI framework to inject it differently.
 *
 * @param controllerClass
 * @tparam A
 * @return
 */
/* def getControllerInstance[A](controllerClass: Class[A]): A = {
  val instance = controllerClass.getConstructors.find { c =>
    val params = c.getParameterTypes
    params.length == 1 && params(0) == classOf[RuntimeEnvironment[DemoUser]]
  }.map {
    _.asInstanceOf[Constructor[A]].newInstance(MyRuntimeEnvironment)
  }
  instance.getOrElse(super.getControllerInstance(controllerClass))
}*/ 