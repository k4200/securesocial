package securesocial.core

import akka.actor.ActorSystem
import play.api.{ Configuration, Environment }
import play.api.cache.AsyncCacheApi
import play.api.i18n.MessagesApi
import securesocial.controllers.{ MailTemplates, ViewTemplates }
import securesocial.core.authenticator._
import securesocial.core.providers._
import securesocial.core.providers.utils.{ Mailer, PasswordHasher, PasswordValidator }
import securesocial.core.services._

import scala.concurrent.ExecutionContext
import scala.collection.immutable.ListMap
import play.api.libs.mailer.MailerClient
import play.api.libs.ws.WSClient
import play.api.mvc.{ PlayBodyParsers, RequestHeader }
/**
 * A runtime environment where the services needed are available
 */
trait RuntimeEnvironment {

  type U

  def wsClient: WSClient
  def cacheApi: AsyncCacheApi
  def environment: Environment
  def mailerClient: MailerClient
  def actorSystem: ActorSystem

  def routes: RoutesService

  def viewTemplates: ViewTemplates
  def mailTemplates: MailTemplates

  def mailer: Mailer

  def currentHasher: PasswordHasher
  def passwordHashers: Map[String, PasswordHasher]
  def passwordValidator: PasswordValidator

  def httpService: HttpService
  def cacheService: CacheService
  def avatarService: Option[AvatarService]

  def customProviders: Map[String, IdentityProvider]
  def providerIds: List[String]

  def idGenerator: IdGenerator
  def authenticatorService: AuthenticatorService[U]

  def eventListeners: Seq[EventListener]

  def userService: UserService[U]

  implicit def executionContext: ExecutionContext

  def configuration: Configuration
  lazy val usernamePasswordConfig: UsernamePasswordConfig =
    UsernamePasswordConfig.fromConfiguration(configuration)
  lazy val httpHeaderConfig: HttpHeaderConfig =
    HttpHeaderConfig.fromConfiguration(configuration)
  lazy val cookieConfig: CookieConfig =
    CookieConfig.fromConfiguration(configuration)
  lazy val enableRefererAsOriginalUrl: EnableRefererAsOriginalUrl =
    EnableRefererAsOriginalUrl(configuration)
  lazy val registrationEnabled =
    RegistrationEnabled(configuration)

  def messagesApi: MessagesApi

  def parsers: PlayBodyParsers

  /**
   * Factory method for IdentityProvider
   * @param provider provider name e.g. "github"
   * @param customOAuth2Settings Valid only for OAuth2Provider. If None, the default settings are used.
   * @return
   */
  def createProvider(provider: String, customOAuth2Settings: Option[OAuth2Settings] = None, miscParam: Option[String] = None, request: Option[RequestHeader] = None): IdentityProvider = {
    provider match {
      case FacebookProvider.Facebook =>
        new FacebookProvider(routes, cacheService, oauth2ClientFor(FacebookProvider.Facebook, customOAuth2Settings))
      case FoursquareProvider.Foursquare =>
        new FoursquareProvider(routes, cacheService, oauth2ClientFor(FoursquareProvider.Foursquare, customOAuth2Settings))
      case GitHubProvider.GitHub =>
        new GitHubProvider(routes, cacheService, oauth2ClientFor(GitHubProvider.GitHub, customOAuth2Settings))
      case GoogleProvider.Google =>
        new GoogleProvider(routes, cacheService, oauth2ClientFor(GoogleProvider.Google, customOAuth2Settings))
      case InstagramProvider.Instagram =>
        new InstagramProvider(routes, cacheService, oauth2ClientFor(InstagramProvider.Instagram, customOAuth2Settings))
      case ConcurProvider.Concur =>
        new ConcurProvider(routes, cacheService, oauth2ClientFor(ConcurProvider.Concur, customOAuth2Settings))
      case SoundcloudProvider.Soundcloud =>
        new SoundcloudProvider(routes, cacheService, oauth2ClientFor(SoundcloudProvider.Soundcloud, customOAuth2Settings))
      case LinkedInOAuth2Provider.LinkedIn =>
        new LinkedInOAuth2Provider(routes, cacheService, oauth2ClientFor(LinkedInOAuth2Provider.LinkedIn, customOAuth2Settings))
      case VkProvider.Vk =>
        new VkProvider(routes, cacheService, oauth2ClientFor(VkProvider.Vk, customOAuth2Settings))
      case DropboxProvider.Dropbox =>
        new DropboxProvider(routes, cacheService, oauth2ClientFor(DropboxProvider.Dropbox, customOAuth2Settings))
      case WeiboProvider.Weibo =>
        new WeiboProvider(routes, cacheService, oauth2ClientFor(WeiboProvider.Weibo, customOAuth2Settings))
      case SpotifyProvider.Spotify =>
        new SpotifyProvider(routes, cacheService, oauth2ClientFor(SpotifyProvider.Spotify, customOAuth2Settings))
      case SlackProvider.Slack =>
        new SlackProvider(routes, cacheService, oauth2ClientFor(SlackProvider.Slack, customOAuth2Settings))
      case BitbucketProvider.Bitbucket =>
        BitbucketProvider(routes, cacheService, oauth2ClientFor(BitbucketProvider.Bitbucket, customOAuth2Settings))
      case BacklogProvider.Backlog =>
        new BacklogProvider(routes, cacheService, oauth2ClientFor(BacklogProvider.Backlog, customOAuth2Settings, miscParam, request))
      case LinkedInProvider.LinkedIn =>
        new LinkedInProvider(routes, cacheService, oauth1ClientFor(LinkedInProvider.LinkedIn))
      case TwitterProvider.Twitter =>
        new TwitterProvider(routes, cacheService, oauth1ClientFor(TwitterProvider.Twitter))
      case XingProvider.Xing =>
        new XingProvider(routes, cacheService, oauth1ClientFor(XingProvider.Xing))
      case ChatWorkProvider.ChatWork =>
        new ChatWorkProvider(routes, cacheService, oauth2ClientFor(ChatWorkProvider.ChatWork, customOAuth2Settings))
      case UsernamePasswordProvider.UsernamePassword =>
        new UsernamePasswordProvider[U](userService, avatarService, viewTemplates, passwordHashers, messagesApi)
      case _ => throw new RuntimeException(s"Invalid provider '$provider'")
    }
  }

  protected def oauth1ClientFor(provider: String) = new OAuth1Client.Default(ServiceInfoHelper.forProvider(configuration, provider), httpService)
  protected def oauth2ClientFor(provider: String, customSettings: Option[OAuth2Settings] = None, miscParam: Option[String] = None, request: Option[RequestHeader] = None): OAuth2Client = {
    val settings = customSettings.getOrElse(OAuth2Settings.forProvider(configuration, provider))
    provider match {
      case ChatWorkProvider.ChatWork =>
        new ChatWorkOAuth2Client(httpService, settings)
      case BacklogProvider.Backlog =>
        new BacklogOAuth2Client(httpService, settings, BacklogProvider.createBacklogApiSettings(cacheService, miscParam, request))
      case _ => new OAuth2Client.Default(httpService, settings)
    }
  }
}

object RuntimeEnvironment {

  /**
   * A default runtime environment.  All built in services are included.
   * You can start your app with with by only adding a userService to handle users.
   */
  abstract class Default extends RuntimeEnvironment {
    override lazy val routes: RoutesService = new RoutesService.Default(environment, configuration)

    override lazy val viewTemplates: ViewTemplates = new ViewTemplates.Default(this)(configuration)
    override lazy val mailTemplates: MailTemplates = new MailTemplates.Default(this)
    override lazy val mailer: Mailer = new Mailer.Default(mailTemplates, mailerClient, configuration, actorSystem)

    override lazy val currentHasher: PasswordHasher = new PasswordHasher.Default(configuration)
    override lazy val passwordHashers: Map[String, PasswordHasher] = Map(currentHasher.id -> currentHasher)
    override lazy val passwordValidator: PasswordValidator = new PasswordValidator.Default(usernamePasswordConfig.minimumPasswordLength)

    override lazy val httpService: HttpService = new HttpService.Default(wsClient)
    override lazy val cacheService: CacheService = new CacheService.Default(cacheApi)
    override lazy val avatarService: Option[AvatarService] = Some(new AvatarService.Default(httpService))
    override lazy val idGenerator: IdGenerator = new IdGenerator.Default(configuration)

    override lazy val authenticatorService: AuthenticatorService[U] = new AuthenticatorService(
      new CookieAuthenticatorBuilder[U](new AuthenticatorStore.Default(cacheService), idGenerator, cookieConfig),
      new HttpHeaderAuthenticatorBuilder[U](new AuthenticatorStore.Default(cacheService), idGenerator, httpHeaderConfig))

    override lazy val eventListeners: Seq[EventListener] = Seq()

    protected def include(p: IdentityProvider): (String, IdentityProvider) = p.id -> p

    override lazy val customProviders: ListMap[String, IdentityProvider] = ListMap()

    override lazy val providerIds = List(
      FacebookProvider.Facebook,
      FoursquareProvider.Foursquare,
      GitHubProvider.GitHub,
      GoogleProvider.Google,
      InstagramProvider.Instagram,
      ConcurProvider.Concur,
      SoundcloudProvider.Soundcloud,
      LinkedInOAuth2Provider.LinkedIn,
      VkProvider.Vk,
      DropboxProvider.Dropbox,
      WeiboProvider.Weibo,
      ConcurProvider.Concur,
      SpotifyProvider.Spotify,
      SlackProvider.Slack,
      BitbucketProvider.Bitbucket,
      BacklogProvider.Backlog,
      //LinkedInProvider.LinkedIn,
      TwitterProvider.Twitter,
      XingProvider.Xing,
      ChatWorkProvider.ChatWork,
      UsernamePasswordProvider.UsernamePassword
    )
  }
}
