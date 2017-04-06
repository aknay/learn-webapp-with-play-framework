package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import play.api.mvc.Flash
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import dao.{AdminToolDao, AlbumDao, UserDao}
import forms.Forms
import org.joda.time.DateTime
import utils.Silhouette.{AuthController, MyEnv}
import scala.concurrent.Future

/**
  * Created by aknay on 5/12/2016.
  */
/** we need to use I18nSupport because we are using form helper
  * Without this --> Form: could not find implicit value for parameter messages: play.api.i18n.Messages
  * Ref: http://stackoverflow.com/questions/30799988/play-2-4-form-could-not-find-implicit-value-for-parameter-messages-play-api-i
  * */
class AlbumController @Inject()(albumDao: AlbumDao, userDao: UserDao, adminToolDao: AdminToolDao)
                               (val messagesApi: MessagesApi, val silhouette: Silhouette[MyEnv])
  extends AuthController with I18nSupport {
  /** we can use album form directly with Album case class by applying id as Option[Long] */

  def isItAllowedToModify: Future[Boolean] = {
    val deadline: Future[(Option[DateTime], Option[DateTime])] = for {
      adminTool <- adminToolDao.getAdminTool
    } yield (adminTool.get.startingDate, adminTool.get.endingDate)

    deadline.map {
      case (None, None) => false
      case (Some(_), Some(_)) => true
    }
  }

  def add = SecuredAction.async { implicit request =>
    val user = Some(request.identity)
    isItAllowedToModify.map {
      case true => Ok(views.html.AlbumView.add(user))
      case false => Ok(views.html.AlbumView.notallowed(user))
    }
  }

  def delete(id: Long) = SecuredAction.async { implicit request =>
    isItAllowedToModify.map {
      case true => {
        albumDao.delete(id)
        Redirect(routes.UserController.user())
      }
      case false => Ok(views.html.AlbumView.notallowed(Some(request.identity)))
    }
  }

  def update(id: Long) = SecuredAction { implicit request =>
    val newAlbumForm = Forms.albumForm.bindFromRequest()
    newAlbumForm.fold(
      hasErrors = { form =>
        println("we are having error, try to check form data is matched with html")
        println(form.data)
        Redirect(routes.HomeController.index())
      },
      success = { _ =>
        albumDao.update(id, newAlbumForm.get, request.identity.id.get)
        Redirect(routes.UserController.user())
      })
  }

  def edit(id: Long) = SecuredAction.async { implicit request =>
    val user = Some(request.identity)
    isItAllowedToModify.flatMap {
      case true => albumDao.find(id).map {
        album => Ok(views.html.AlbumView.edit(user, id, Forms.albumForm.fill(album)))
      }
      case false => Future.successful(Ok(views.html.AlbumView.notallowed(user)))
    }
  }

  def save = SecuredAction.async { implicit request =>

    val newProductForm = Forms.albumForm.bindFromRequest()

    newProductForm.fold(
      hasErrors = { form =>
        println("we are having error, try to check form data is matched with html")
        println(form.data)
        Future.successful(Redirect(routes.HomeController.index()))
      },
      success = {
        newAlbum =>
          albumDao.insertAlbum(newAlbum, request.identity.id.get).map {
            case true => Redirect(routes.UserController.user())
            case false => Redirect(routes.AlbumController.add()) flashing (Flash(newProductForm.data) +
              ("error" -> Messages("album.alreadyExisted.error")))
          }
      })
  }

}