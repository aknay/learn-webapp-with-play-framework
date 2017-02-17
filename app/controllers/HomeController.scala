package controllers

import javax.inject._

import play.api.i18n.MessagesApi
import play.api.mvc.Action
import com.mohiva.play.silhouette.api.Silhouette
import utils.Silhouette.{AuthController, MyEnv}

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()()
                              (val messagesApi: MessagesApi,
                               val silhouette: Silhouette[MyEnv])
  extends AuthController {

  def index = UserAwareAction { implicit request =>
    request.identity match {
      case Some(user) => Ok(views.html.index(Some(user)))
      case None =>        Ok(views.html.index())
    }
  }

  def show(userName: String) = Action {
    Ok(views.html.users(userName))
  }
}
