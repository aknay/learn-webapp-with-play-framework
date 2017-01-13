package controllers

import javax.inject.Inject

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.i18n.{I18nSupport, MessagesApi}
import dao.{AlbumDao, UserDao}
import models.{Album, User}
import scala.concurrent.Future

/**
  * Created by aknay on 5/12/2016.
  */
/** we need to use I18nSupport because we are using form helper
  * Without this --> Form: could not find implicit value for parameter messages: play.api.i18n.Messages
  * Ref: http://stackoverflow.com/questions/30799988/play-2-4-form-could-not-find-implicit-value-for-parameter-messages-play-api-i
  * */
class AlbumController @Inject()(albumDao: AlbumDao, userDao: UserDao)(val messagesApi: MessagesApi) extends Controller with I18nSupport {
  /** we can use album form directly with Album case class by applying id as Option[Long] */
  val albumForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "userId" -> optional(longNumber),
      "artist" -> nonEmptyText,
      "title" -> nonEmptyText

    )(Album.apply)(Album.unapply)
  )

  def albumOverview = Action { implicit request =>
    Ok(views.html.Album.album())
  }

  def listAllAlbum = Action.async { implicit request =>
    request.session.get("connected").map {
      emailAddress =>
        val loginUser: User = userDao.getUserByEmailAddress(emailAddress).get
        albumDao.retrieveAlbumByUserId(loginUser.id.get).map {
          albums => Ok(views.html.Album.albumlist(albums))
        }
    }.getOrElse {
      Future.successful(Unauthorized("Oops, you are not connected"))
    }
  }

  def delete(id: Long) = Action { implicit request =>
    albumDao.delete(id)
    Redirect(routes.AlbumController.listAllAlbum())
  }

  def update(id: Long) = Action { implicit request =>
    val newAlbumForm = albumForm.bindFromRequest()
    newAlbumForm.fold(
      hasErrors = { form =>
        println("we are having error, try to check form data is matched with html")
        println(form.data)
        Redirect(routes.HomeController.index())
      },
      success = {
        newAlbum =>
          request.session.get("connected").map { emailAddress =>
            val loginUser: User = userDao.getUserByEmailAddress(emailAddress).get
            albumDao.update(id, newAlbumForm.get, loginUser.id.get)
            Redirect(routes.AlbumController.listAllAlbum())
          }.getOrElse {
            Unauthorized("Oops, you are not connected")
          }
      })
  }

  def edit(id: Long) = Action { implicit request =>
    val album: Album = albumDao.find(id)
    val form: Form[Album] = albumForm.fill(album)
    Ok(views.html.Album.edit(id, form))
  }


  def insert = Action { implicit request =>

    val newProductForm = albumForm.bindFromRequest()

    newProductForm.fold(
      hasErrors = { form =>
        println("we are having error, try to check form data is matched with html")
        println(form.data)
        Redirect(routes.HomeController.index())
      },
      success = {
        newAlbum =>
          request.session.get("connected").map { emailAddress =>
            val loginUser: User = userDao.getUserByEmailAddress(emailAddress).get
            albumDao.insertAlbum(newAlbum, loginUser.id.get)
            Redirect(routes.AlbumController.listAllAlbum())

          }.getOrElse {
            Unauthorized("Oops, you are not connected")
          }
      })
  }

}
