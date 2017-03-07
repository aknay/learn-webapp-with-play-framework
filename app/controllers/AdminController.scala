package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{Clock, PasswordHasherRegistry}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import dao.{AlbumDao, UserDao}
import forms.Forms
import models._
import play.api.Configuration
import play.api.i18n.{I18nSupport, MessagesApi}
import utils.Mailer
import utils.Silhouette._

import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by aknay on 3/3/17.
  */
class AdminController @Inject()(userDao: UserDao,
                                albumDao: AlbumDao,
                                albumController: AlbumController,
                                userService: UserService,
                                authInfoRepository: AuthInfoRepository,
                                credentialsProvider: CredentialsProvider,
                                tokenService: MailTokenService[MailTokenUser],
                                masterTokenService: MailTokenService[MailTokenMasterUser],
                                mailer: Mailer,
                                conf: Configuration,
                                clock: Clock,
                                passwordHasherRegistry: PasswordHasherRegistry)
                               (val messagesApi: MessagesApi,
                                val silhouette: Silhouette[MyEnv])
  extends AuthController with I18nSupport {


  def admin = SecuredAction(WithServices(Role.Admin)) { implicit request =>
    Ok(views.html.Admin.profile(request.identity))
  }

  def viewAllNonAdminUser = SecuredAction(WithServices(Role.Admin)) { implicit request =>
    Ok(views.html.Admin.viewallnonadminuser(request.identity, userDao.getNonAdminUserList()))
  }

  def viewAllAlbumsFromNonAdminUser(userId: Long, page: Int = 0) = SecuredAction(WithServices(Role.Admin)).async { implicit request =>
    albumDao.listWithPage(userId, page).map {
      (albums: Page[Album]) => Ok(views.html.AlbumView.list(albums, userId = userId))
    }
  }

  def viewAnnouncementForm = UnsecuredAction { implicit request =>
    Ok(views.html.Admin.MakeAnnouncement(Forms.announcementForm))
  }

}
