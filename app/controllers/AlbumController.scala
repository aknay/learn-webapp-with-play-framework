package controllers

import javax.inject.Inject
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import dao.AlbumDao
import models.AlbumFormData

/**
  * Created by aknay on 5/12/2016.
  */

class AlbumController @Inject()(albumDao: AlbumDao) extends Controller {
  //Note: Form use AlbumFormData instead of Album case class directly
  val albumForm = Form(
    mapping(
      "artist" -> nonEmptyText,
      "title" -> nonEmptyText
    )(AlbumFormData.apply)(AlbumFormData.unapply)
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
