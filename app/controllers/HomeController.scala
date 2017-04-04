package controllers

import javax.inject._

import play.api.i18n.MessagesApi
import play.api.mvc.Action
import com.mohiva.play.silhouette.api.Silhouette
import dao.{AdminToolDao, UserDao}
import utils.Silhouette.{AuthController, MyEnv}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(userDao: UserDao,
                               adminToolDao: AdminToolDao)
                              (val messagesApi: MessagesApi,
                               val silhouette: Silhouette[MyEnv])
  extends AuthController {

  userDao.createUserInfoTableIfNotExisted

  def index = UserAwareAction.async { implicit request =>
//    var announcement = Option("")
//    var startingDate = Option("")
//    var endingDate = Option("")
//    val tool = adminToolDao.getAdminTool
//
//    announcement = None
//    startingDate = None
//    endingDate = None

//    if (adminToolDao.getAdminTool.isDefined) {
//      if (adminToolDao.getAdminTool.get.announcement.isDefined) {
//        announcement = tool.get.announcement
//        startingDate = Some(adminToolDao.getFormattedDateString(tool.get.startingDate.get))
//        endingDate = Some(adminToolDao.getFormattedDateString(tool.get.endingDate.get))
//      }
//    }
   val t =  for {
      startingDate <- adminToolDao.getStartingDate
      endingDate <- adminToolDao.getEndingDate
      announcement <- adminToolDao.getAnnouncement
    }yield (startingDate,endingDate, announcement)


    //val user = request.identity
    //TODO : Ok(views.html.index(None, a.get, Some(adminToolDao.getFormattedDateString(s.get.get)), Some(adminToolDao.getFormattedDateString(e.get.get))))
    t.map{
      case (startingDate, endingDate, announcement) =>
        (startingDate, endingDate, announcement) match {
          case (s,e,a) => Ok(views.html.index(None, a.get, Some(adminToolDao.getFormattedDateString(s.get.get)), Some(adminToolDao.getFormattedDateString(e.get.get))))
          case (None, None, None) => Ok(views.html.index(None, None, None, None))
        }
    }




//    request.identity match {
//      case Some(user) => Ok(views.html.index(Some(user), announcement, startingDate, endingDate))
//      case None => Ok(views.html.index(None, announcement, startingDate, endingDate))
//    }
  }

  def show(userName: String) = Action {
    Ok(views.html.users(userName))
  }
}
