package controllers

import javax.inject._
import dao.AlbumDao
import javax.inject.Inject
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import play.api.mvc.Controller

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(albumDao: AlbumDao) extends Controller {
  /**
    * Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */

  def index = Action {
    Ok(views.html.index())
  }

  def show(userName: String) = Action {
    Ok(views.html.users(userName))
  }

  def listAllAlbum = Action.async { implicit request =>
    albumDao.createTableIfNotExisted
    albumDao.getAlbumTable.map { case albums =>
      Ok(views.html.Album.albumlist(albums))
    }

  }

}
