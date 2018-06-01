/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package service

import javax.inject.{ Inject, Singleton }

import akka.actor.ActorSystem
import play.api.{ Configuration, Environment }
import play.api.cache.CacheApi
import play.api.i18n.MessagesApi
import play.api.libs.mailer.MailerClient
import play.api.libs.ws.WSClient
import securesocial.core.{ ServiceInfoHelper, RuntimeEnvironment }
import securesocial.core.authenticator.{ HttpHeaderAuthenticatorConfigurations, CookieAuthenticatorConfigurations }
import securesocial.core.providers.UsernamePasswordProviderConfigurations
import securesocial.core.services.UserService

@Singleton
class MyEnvironment @Inject() (override val configuration: Configuration, implicit val playEnv: Environment, val cacheApi: CacheApi, override val messagesApi: MessagesApi, val WS: WSClient, val mailerClient: MailerClient, val actorSystem: ActorSystem) extends RuntimeEnvironment.Default {
  type U = DemoUser
  override val userService: UserService[U] = new InMemoryUserService()

  val cookieAuthenticatorConfigurations = new CookieAuthenticatorConfigurations.Default(configuration, playEnv)
  val httpHeaderAuthenticatorConfigurations = new HttpHeaderAuthenticatorConfigurations.Default(configuration, playEnv)
  val serviceInfoHelper = new ServiceInfoHelper.Default
  val usernamePasswordProviderConfigurations = new UsernamePasswordProviderConfigurations.Default(configuration)

}