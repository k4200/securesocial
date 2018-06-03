/**
 * Copyright 2017 Takeshi Kimura
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
package securesocial.core.providers

import play.api.libs.ws.WSAuthScheme
import securesocial.core._
import securesocial.core.services.{ CacheService, HttpService, RoutesService }

import scala.concurrent.{ ExecutionContext, Future }
import ChatWorkProvider._

class ChatWorkOAuth2Client(
    httpService: HttpService, settings: OAuth2Settings
)(implicit executionContext: ExecutionContext) extends OAuth2Client.Default(httpService, settings)(executionContext) {
  override def exchangeCodeForToken(code: String, callBackUrl: String, builder: OAuth2InfoBuilder): Future[OAuth2Info] = {
    val params = Map(
      OAuth2Constants.GrantType -> Seq(OAuth2Constants.AuthorizationCode),
      OAuth2Constants.Code -> Seq(code),
      OAuth2Constants.RedirectUri -> Seq(callBackUrl)
    ) ++ settings.accessTokenUrlParams.mapValues(Seq(_))
    httpService.url(settings.accessTokenUrl)
      .withAuth(settings.clientId, settings.clientSecret, WSAuthScheme.BASIC)
      .post(params).map(builder)
  }

}
/**
 * A ChatWork provider
 *
 */
class ChatWorkProvider(
  routesService: RoutesService,
  cacheService: CacheService,
  client: OAuth2Client
)
    extends OAuth2Provider(routesService, client, cacheService) {
  override val id = ChatWorkProvider.ChatWork

  override def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    val accessToken = info.accessToken
    client.httpService.url(ChatWorkProvider.Api).withHeaders("Authorization" -> s"Bearer $accessToken").get().map { response =>
      response.status match {
        case 200 =>
          val data = response.json
          val accountId = (data \ AccountId).as[Int].toString()
          val chatworkId = (data \ ChatWorkId).asOpt[String]
          val fullName = (data \ Name).asOpt[String]
          val email = (data \ Email).asOpt[String]
          val extraInfo = Map(
            ChatWorkId -> chatworkId.getOrElse("")
          )
          BasicProfile(id, accountId, None, None, fullName, email, None, authMethod, None, Some(info), extraInfo = Some(extraInfo))
        case _ =>
          logger.error("[securesocial] ChatWork account info request returned error: " + response.body)
          throw new AuthenticationException()
      }
    } recover {
      case e =>
        logger.error("[securesocial] error retrieving profile information from ChatWork", e)
        throw new AuthenticationException()
    }
  }
}

object ChatWorkProvider {
  val Api = "https://api.chatwork.com/v2/me"
  val ChatWork = "chatwork"
  val AccountId = "account_id"
  val ChatWorkId = "chatwork_id"
  val Name = "name"
  val Url = "url"
  val Email = "login_mail"

  case class ErrorResponse(
    message: String,
    detail: String,
    id: Option[String]
  )

  case class UserResponse(
    uuid: String,
    display_name: String,
    username: String
  )
}
