package controllers

import javax.inject.Inject

import play.api.data.Forms._
import dao.UserDao
import models.User
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}

/**
  * Created by aknay on 27/12/16.
  */
class UserController @Inject()(userDao: UserDao)(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  val userForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "email" -> nonEmptyText,
      "password" -> nonEmptyText
    )(User.apply)(User.unapply)
  )

  def login = Action { implicit request =>
    Ok(views.html.User.login(userForm))

  }

  def signUp = Action { implicit request =>
    Ok(views.html.User.signup(userForm))
  }

  def loginCheck = Action { implicit request =>
    val loginForm = userForm.bindFromRequest()
    loginForm.fold(
      hasErrors = { form =>
        println("we are having error, try to check form data is matched with html")
        println(form.data)
        Redirect(routes.UserController.login())
      },
      success = {
        userFromForm =>

          if (userDao.checkUser(userFromForm)) {
            /** user form knows nothing about user id so I need get id from database */
            val temp = userDao.findByEmailAddress(userFromForm.email)
            Redirect(routes.UserController.profile(temp.get.id.get)).withSession(
              "connected" -> userFromForm.email)
          }
          else {
            Redirect(routes.UserController.login())
          }
      })
  }

  def logout = Action { request =>
    Redirect(routes.HomeController.index()).withNewSession
  }

  def profile(id: Long) = Action { request =>
    val user = userDao.findById(id)
    if (user.isEmpty) {
      Unauthorized("No such user")
    }
    else {
      request.session.get("connected").map { emailAddress =>
        val loginUser: User = userDao.findByEmailAddress(emailAddress).get
        val tempId: Long = loginUser.id.get
        if (id != loginUser.id.get) {
          Unauthorized("Oops, you are not connected")
        }
        else {
          Ok(views.html.User.profile(loginUser))
        }
      }.getOrElse {
        Unauthorized("Oops, you are not connected")
      }
    }
  }

  def signUpCheck = Action { implicit request =>
    val signUpForm = userForm.bindFromRequest()

    signUpForm.fold(
      hasErrors = { form =>
        println("we are having error, try to check form data is matched with html")
        println(form.data)
        Redirect(routes.HomeController.index())
      },
      success = {
        user =>
          if (userDao.isUserExisted(user.email)) Redirect(routes.AlbumController.listAllAlbum())
          else {
            userDao.signUp(user)
            Redirect(routes.HomeController.index())
          }
      })
  }

  def authorize = Action { implicit request =>
    val newProductForm = userForm.bindFromRequest()

    newProductForm.fold(
      hasErrors = { form =>
        println("we are having error, try to check form data is matched with html")
        println(form.data)
        Redirect(routes.HomeController.index())
      },
      success = {
        newProduct =>
          Redirect(routes.AlbumController.listAllAlbum())
      })
  }

}
