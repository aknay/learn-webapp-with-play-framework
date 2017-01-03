package controllers

import javax.inject.Inject
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.i18n.{I18nSupport, MessagesApi}

import dao.AlbumDao
import models.Album

/**
  * Created by aknay on 5/12/2016.
  */
/** we need to use I18nSupport because we are using form helper
  * Without this --> Form: could not find implicit value for parameter messages: play.api.i18n.Messages
  * Ref: http://stackoverflow.com/questions/30799988/play-2-4-form-could-not-find-implicit-value-for-parameter-messages-play-api-i
  * */
class AlbumController @Inject()(albumDao: AlbumDao)(val messagesApi: MessagesApi) extends Controller with I18nSupport {
  /** we can use album form directly with Album case class by applying id as Option[Long] */
  val albumForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "artist" -> nonEmptyText,
      "title" -> nonEmptyText
    )(Album.apply)(Album.unapply)
  )

  def albumOverview = Action { implicit request =>
    Ok(views.html.Album.album())
  }

  def listAllAlbum = Action.async { implicit request =>
    albumDao.createTableIfNotExisted
    albumDao.getAlbumTable.map { case albums =>
      Ok(views.html.Album.albumlist(albums))
    }
  }

  def delete(id: Long) = Action { implicit request =>
    albumDao.delete(id)
    Redirect(routes.AlbumController.listAllAlbum())
  }

  def update(id: Long) = Action { implicit request =>
    val albumFormData: Album = albumForm.bindFromRequest().get
    albumDao.update(id, albumFormData)
    Redirect(routes.AlbumController.listAllAlbum())
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
        newProduct =>
          albumDao.insertAlbum(newProduct)
          Redirect(routes.AlbumController.listAllAlbum())
      })
  }

}
