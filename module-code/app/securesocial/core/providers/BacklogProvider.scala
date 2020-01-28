/**
 * This file is based on SlackProvider.scala, which is based on GitHubProvider.scala
 * Original work: Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 * Modifcations: Copyright 2016 KASHIMA Kazuo (k4200 at kazu dot tv) - twitter: @k4200
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
package securesocial.core.providers

import _root_.java.util.UUID

import play.api.libs.ws.{ WSRequest, WSResponse }
import play.api.libs.json.{ Reads, Json, JsValue }
import play.api.mvc._
import securesocial.core._
import securesocial.core.services.{ CacheService, HttpService, RoutesService }

import scala.concurrent.duration.{ Duration, _ }
import scala.concurrent.{ Await, ExecutionContext, Future }

import BacklogProvider._

class BacklogOAuth2Client(
  baaseHttpService: HttpService, settings: OAuth2Settings, backlogApiSettings: BacklogApiSettings)(implicit executionContext: ExecutionContext)
  extends OAuth2Client.Default(new BacklogHttpService(baaseHttpService, backlogApiSettings), backlogApiSettings.getOAuth2Settings(settings)) {

  def retrieveProfile(profileUrl: String, accessToken: String): Future[JsValue] = {
    httpService.url(profileUrl)
      .withHttpHeaders("Authorization" -> s"Bearer $accessToken")
      .get().map(_.json)
  }

  def apiHost: String = backlogApiSettings.checkedApiHost()

  def getUrlForSpace(url: String): String = backlogApiSettings.getUrlForSpace(url)
}

class BacklogHttpService(httpService: HttpService, backlogApiSettings: BacklogApiSettings) extends HttpService {

  def url(url: String): WSRequest = {
    // ensure apiHost is available
    backlogApiSettings.checkedApiHost()
    httpService.url(url)
  }
}

/**
 * A Backlog provider
 *  @param routesService
 *  @param cacheService
 *  @param client
 */
class BacklogProvider(
  routesService: RoutesService,
  cacheService: CacheService,
  client: OAuth2Client)
  extends OAuth2Provider(routesService, client, cacheService) {

  private val getAuthenticatedUserUrl = "https://{apiHost}/api/v2/users/myself"
  val AccessToken = "token"

  implicit val errorReads: Reads[Error] = Json.reads[Error]
  implicit val errorResponseReads: Reads[ErrorResponse] = Json.reads[ErrorResponse]
  implicit val authTestReads: Reads[AuthTestResponse] = Json.reads[AuthTestResponse]

  val id = BacklogProvider.Backlog

  val backlogClient: BacklogOAuth2Client = client match {
    case client: BacklogOAuth2Client => client
    case _ =>
      logger.error("[securesocial] error to get a BacklogOAuth2Client instance")
      throw new AuthenticationException()
  }

  override protected def buildInfo(response: WSResponse): OAuth2Info = {
    val er = response.json.asOpt[ErrorResponse]
    if (er.isDefined) {
      logger.error("[securesocial] An error occurred while getting an access token: " + er.get.messages)
      throw new AuthenticationException()
    }
    super.buildInfo(response)
  }

  def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    val url = backlogClient.getUrlForSpace(getAuthenticatedUserUrl)
    logger.debug(s"[securesocial] getting profile info from $url")
    backlogClient.retrieveProfile(url, info.accessToken).map { me =>
      logger.debug("[securesocial] got response: " + Json.stringify(me))
      val errorResponse = me.asOpt[ErrorResponse]
      errorResponse.map { error =>
        logger.error(s"[securesocial] error retrieving profile information from Backlog: " + error.messages)
        throw new AuthenticationException()
      }.getOrElse {
        val userInfo = me.as[AuthTestResponse]
        val extraInfo = Map(
          "space_host_name" -> backlogClient.apiHost)
        BasicProfile(id, userInfo.id.toString, None, None, Some(userInfo.userId), Some(userInfo.mailAddress), None, authMethod, oAuth2Info = Some(info), extraInfo = Some(extraInfo))
      }
    } recover {
      case e: AuthenticationException => throw e
      case e =>
        logger.error("[securesocial] error retrieving profile information from Backlog", e)
        throw new AuthenticationException()
    }
  }

  override def authenticate()(implicit request: Request[AnyContent]): Future[AuthenticationResult] = {
    val maybeError = request.queryString.get(OAuth2Constants.Error).flatMap(_.headOption).map {
      case OAuth2Constants.AccessDenied => Future.successful(AuthenticationResult.AccessDenied())
      case error =>
        Future.failed {
          logger.error(s"[securesocial] error '$error' returned by the authorization server. Provider is $id")
          throw new AuthenticationException()
        }
    }
    maybeError.getOrElse {
      request.queryString.get(OAuth2Constants.Code).flatMap(_.headOption) match {
        case Some(code) =>
          // we're being redirected back from the authorization server with the access code.
          val sessionId = request.session.get(IdentityProvider.SessionId).getOrElse {
            logger.error("[securesocial] missing sid in session.")
            throw new AuthenticationException()
          }
          val result = for {
            // check if the state we sent is equal to the one we're receiving now before continuing the flow.
            // todo: review this -> clustered environments
            stateOk <- cacheService.getAs[Tuple2[String, String]](sessionId).map { optT =>
              (optT.map {
                case (originalState, _) =>
                  val stateInQueryString = request.queryString.get(OAuth2Constants.State).flatMap(_.headOption).getOrElse(throw new AuthenticationException())
                  originalState == stateInQueryString
              }).getOrElse {
                false
              }
            }
            oAuth2Info <- getAccessToken(code) if stateOk
            user <- fillProfile(oAuth2Info)
          } yield {
            logger.debug(s"[securesocial] user loggedin using provider $id = $user")
            AuthenticationResult.Authenticated(user)
          }
          result recover {
            case e =>
              logger.error("[securesocial] error authenticating user", e)
              throw e
          }
        case None =>
          // There's no code in the request, this is the first step in the oauth flow
          val state = UUID.randomUUID().toString
          val sessionId = request.session.get(IdentityProvider.SessionId).getOrElse(UUID.randomUUID().toString)
          val miscParam = request.queryString.get("miscParam").flatMap(_.headOption).getOrElse {
            logger.error("[securesocial] miscParam is missing.")
            throw new AuthenticationException()
          }
          cacheService.set(sessionId, (state, miscParam), 300).map {
            unit =>
              val url = client.navigationFlowUrl(routesService.authenticationUrl(id), state)
              logger.debug("[securesocial] redirecting to: [%s]".format(url))
              AuthenticationResult.NavigationFlow(Results.Redirect(url).withSession(request.session + (IdentityProvider.SessionId -> sessionId)))
          }
      }
    }
  }
}

object BacklogProvider {
  val Backlog = "backlog"
  case class Error(
    message: String,
    code: Int,
    moreInfo: String)
  case class ErrorResponse(
    errors: List[Error]) {
    def messages = {
      errors.map(_.message).mkString(",")
    }
  }
  case class AuthTestResponse(
    id: Int,
    userId: String,
    roleType: Int,
    lang: Option[String],
    mailAddress: String)

  /**
   * Create a BacklogApiSettings.
   *
   *  @param cacheService
   *  @param optSpaceIdOrApiHost One of the following:
   *  <ul>
   *    <li>Some("space-id.backlogtool.com")</li>
   *    <li>Some("space-id")</li>
   *    <li>None (when the data is in the session)</li>
   *  </ul>
   */
  def createBacklogApiSettings(
    cacheService: CacheService,
    optSpaceIdOrApiHost: Option[String],
    optRequest: Option[RequestHeader])(implicit executionContext: ExecutionContext): BacklogApiSettings = {
    val awaitTimeout: Duration = 5.seconds

    val apiHost: Option[String] = optSpaceIdOrApiHost.orElse {
      optRequest.flatMap(request => Await.result(getMiscParamFromSession(request, cacheService), awaitTimeout))
    }.map { spaceIdOrApiHost =>
      if (spaceIdOrApiHost.contains(".")) {
        spaceIdOrApiHost
      } else {
        s"${spaceIdOrApiHost}.backlogtool.com"
      }
    }

    BacklogApiSettings(apiHost)
  }

  private def getMiscParamFromSession(
    request: RequestHeader,
    cacheService: CacheService)(implicit executionContext: ExecutionContext): Future[Option[String]] = {
    request.session.get(IdentityProvider.SessionId).map { sessionId =>
      val state = request.queryString.get(OAuth2Constants.State).flatMap(_.headOption)
      cacheService.getAs[(String, String)](sessionId).map {
        case Some((originalState, miscParam)) if state.contains(originalState) =>
          Some(miscParam)
        case _ => None
      } recover {
        case _ => None
      }
    }.getOrElse(Future(None))
  }
}

case class BacklogApiSettings(optApiHost: Option[String]) {

  private val logger = play.api.Logger(this.getClass.getName)

  def getOAuth2Settings(settings: OAuth2Settings): OAuth2Settings = {
    optApiHost.map { apiHost =>
      settings.copy(
        authorizationUrl = getUrlForSpace(settings.authorizationUrl, apiHost),
        accessTokenUrl = getUrlForSpace(settings.accessTokenUrl, apiHost))
    }.getOrElse(settings)
  }

  private def getUrlForSpace(url: String, apiHost: String): String = {
    url.replace("{apiHost}", apiHost)
  }

  def getUrlForSpace(url: String): String = {
    getUrlForSpace(url, checkedApiHost())
  }

  def checkedApiHost(): String = {
    optApiHost.getOrElse {
      logger.error("[securesocial] optApiHost isn't set.")
      throw new AuthenticationException()
    }
  }
}
