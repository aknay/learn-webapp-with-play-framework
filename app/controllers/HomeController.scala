package controllers

import javax.inject._
import play.api._
import play.api.mvc._

import dao.AlbumDao

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
    albumDao.printHello
    albumDao.createTable
    albumDao.insertPredefinedData
    albumDao.printAllDataOnTable

    Ok (views.html.index())


    //Ok(views.html.index("Your new application is ready."))
  }

  def show(userName: String) = Action {
    Ok(views.html.users(userName))
  }

}
