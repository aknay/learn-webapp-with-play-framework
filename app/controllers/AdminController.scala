package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{LoginInfo, Silhouette}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{Clock, PasswordHasherRegistry}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import dao.{AdminToolDao, AlbumDao, UserDao}
import forms.Forms
import models._
import play.api.Configuration
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Flash
import utils.Mailer
import utils.Silhouette._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
  * Created by aknay on 3/3/17.
  */
class AdminController @Inject()(userDao: UserDao,
                                albumDao: AlbumDao,
                                adminToolDao: AdminToolDao,
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

  def viewAnnouncementForm = SecuredAction(WithServices(Role.Admin)) { implicit request =>
    Ok(views.html.Admin.MakeAnnouncement(Forms.announcementForm))
  }

  def viewSuccessfulAnnouncement = SecuredAction(WithServices(Role.Admin)) { implicit request =>
    val user = request.identity
    val startingDate = adminToolDao.getStartingDate(user).get
    val endingDate = adminToolDao.getEndingDate(user).get
    val announcement = adminToolDao.getAnnouncement(user).get

    val formattedStartingDateString = adminToolDao.getFormattedDateString(startingDate)
    val formattedEndingDateString = adminToolDao.getFormattedDateString(endingDate)
    Ok(views.html.Admin.SuccessfulAnnouncement(
      Some(user), formattedStartingDateString, formattedEndingDateString, announcement))
  }

  def announcementCheck = SecuredAction(WithServices(Role.Admin)).async { implicit request =>
    Forms.announcementForm.bindFromRequest.fold(
      formWithError => Future.successful(BadRequest(views.html.Admin.MakeAnnouncement(Forms.announcementForm))
        .flashing(Flash(Forms.announcementForm.data))),
      formData => {
        val user = request.identity
        if (!adminToolDao.isExist(user)) adminToolDao.create(user)
        adminToolDao.setStatingDateAndEndingDate(user, formData.startingDate.get, formData.endingDate.get)
        adminToolDao.setAnnouncement(user, formData.announcement.get)
        Future.successful(Redirect(routes.AdminController.viewSuccessfulAnnouncement()))
      }
    )
  }

}
