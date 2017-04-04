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


  def admin = SecuredAction(WithServices(Role.Admin)).async { implicit request =>
//    val adminTool = adminToolDao.getAnnouncement
//
//    Ok(views.html.Admin.profile(request.identity, adminToolDao.getAdminTool))
//
//
//    for {
//      adminTool <- adminToolDao.getAnnouncement
//
//    }

      adminToolDao.getAdminTool.map{
        adminTool => Ok(views.html.Admin.profile(request.identity, adminTool))
      }

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
    val user = request.identity
    Ok(views.html.Admin.MakeAnnouncement(Some(user), Forms.announcementForm))
  }

  def viewAnnouncement = SecuredAction(WithServices(Role.Admin)).async { implicit request =>
//    val user = request.identity
//    val startingDate = adminToolDao.getStartingDate
//    val endingDate = adminToolDao.getEndingDate
//    val announcement = adminToolDao.getAnnouncement
//
//    if (startingDate.isEmpty || endingDate.isEmpty || announcement.isEmpty) {
//      Ok(views.html.Admin.ViewAnnouncement(Some(user)))
//    }
//    else {
//      val formattedStartingDateString = adminToolDao.getFormattedDateString(startingDate.get)
//      val formattedEndingDateString = adminToolDao.getFormattedDateString(endingDate.get)
//      Ok(views.html.Admin.ViewAnnouncement(
//        Some(user), Some(formattedStartingDateString), Some(formattedEndingDateString), announcement))
//    }
    val user = request.identity
    val result: Future[Boolean] = for {
      startDate <- adminToolDao.getStartingDate
      endingDate <- adminToolDao.getEndingDate
      announcement <- adminToolDao.getAnnouncement
    } yield startDate.get.isDefined && endingDate.get.isDefined && announcement.get.isDefined


    val resultAndOptions  = for {
      startDate <- adminToolDao.getStartingDate
      endingDate <- adminToolDao.getEndingDate
      announcement <- adminToolDao.getAnnouncement
    } yield (startDate, endingDate, announcement)

    resultAndOptions.map{
      case (startingDate, endingDate, announcement) =>
        (startingDate, endingDate, announcement) match {
          case (s,e,a) => Ok(views.html.Admin.ViewAnnouncement(Some(user),
            Some(adminToolDao.getFormattedDateString(s.get.get)), Some(adminToolDao.getFormattedDateString(e.get.get)), announcement.get))
          case (None, None, None) => Ok(views.html.Admin.ViewAnnouncement(Some(user)))
        }
    }




//    result.map{
//      r => {
//        if (r) Ok(views.html.Admin.ViewAnnouncement(Some(user), Some(formattedStartingDateString), Some(formattedEndingDateString), announcement))
//      }
//    }

//    val rt= for {
//      startDate <- adminToolDao.getStartingDate
//      endingDate <- adminToolDao.getEndingDate
//      announcement <- adminToolDao.getAnnouncement
//    } yield {
//      if(startDate.get.isDefined && endingDate.get.isDefined && announcement.get.isDefined)  Ok(views.html.Admin.ViewAnnouncement(Some(user), Some(adminToolDao.getFormattedDateString(startDate.get.get)), Some(adminToolDao.getFormattedDateString(endingDate.get.get)), announcement.get))
//      else Ok
//    }
//


//    rt
//    Ok
  }

  def deleteAnnouncement = SecuredAction(WithService(Role.Admin)) { implicit request =>
//    val isSuccessful = adminToolDao.deleteAnnouncement(request.identity)
    Ok(views.html.Admin.DeleteAnnouncement(true, Some(request.identity)))
  }

  def viewSuccessfulAnnouncement = SecuredAction(WithServices(Role.Admin)).async { implicit request =>


    val user = request.identity
    val result: Future[Boolean] = for {
      startDate <- adminToolDao.getStartingDate
      endingDate <- adminToolDao.getEndingDate
      announcement <- adminToolDao.getAnnouncement
    } yield startDate.get.isDefined && endingDate.get.isDefined && announcement.get.isDefined


    val resultAndOptions  = for {
      startDate <- adminToolDao.getStartingDate
      endingDate <- adminToolDao.getEndingDate
      announcement <- adminToolDao.getAnnouncement
    } yield (startDate, endingDate, announcement)

    resultAndOptions.map{
      case (startingDate, endingDate, announcement) =>
        (startingDate, endingDate, announcement) match {
          case (s,e,a) => Ok(views.html.Admin.SuccessfulAnnouncement(Some(user),
            adminToolDao.getFormattedDateString(s.get.get), adminToolDao.getFormattedDateString(e.get.get), announcement.get.get))
          case (None, None, None) => Ok(views.html.Admin.ViewAnnouncement(Some(user)))
        }
    }









//    val user = request.identity
//    val startingDate = adminToolDao.getStartingDate.get
//    val endingDate = adminToolDao.getEndingDate.get
//    val announcement = adminToolDao.getAnnouncement.get

//    val formattedStartingDateString = adminToolDao.getFormattedDateString(startingDate)
//    val formattedEndingDateString = adminToolDao.getFormattedDateString(endingDate)
//    Ok(views.html.Admin.SuccessfulAnnouncement(
//      Some(user), formattedStartingDateString, formattedEndingDateString, announcement))
  }

  def announcementCheck = SecuredAction(WithServices(Role.Admin)).async { implicit request =>
    Forms.announcementForm.bindFromRequest.fold(
      formWithError => Future.successful(BadRequest(views.html.Admin.MakeAnnouncement(Some(request.identity),Forms.announcementForm))
        .flashing(Flash(Forms.announcementForm.data))),
      formData => {
        val user = request.identity
        //TODO adminToolDao.makeAnnouncement(user, formData.startingDate.get, formData.endingDate.get, formData.announcement.get)
        Future.successful(Redirect(routes.AdminController.viewSuccessfulAnnouncement()))
      }
    )
  }

  def viewEvents = SecuredAction(WithServices(Role.Admin)).async { implicit request =>


    val user = request.identity


    adminToolDao.getEvent.map {
      events => Ok(views.html.Admin.ViewEvents(Some(user), Some(events.get.get.split("\\s+").toList)))
    }

    //    val eventsList = adminToolDao.getEventsAsList
    //    Ok(views.html.Admin.ViewEvents(Some(user), eventsList))
    //  }
  }
}
