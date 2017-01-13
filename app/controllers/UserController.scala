package controllers

import javax.inject.Inject
import play.api.data.Forms._
import dao.UserDao
import models.{User, UserInfo}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, Controller, Flash}

/**
  * Created by aknay on 27/12/16.
  */

object UserController {
  private var mHasLoggedIn = false;

  def hasLoggedIn: Boolean = mHasLoggedIn
}


class UserController @Inject()(userDao: UserDao)(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  val userForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "email" -> nonEmptyText,
      "password" -> nonEmptyText
    )(User.apply)(User.unapply)
  )

  def login = Action { implicit request =>
    val form = if (request.flash.get("error").isDefined) {
      val errorForm = userForm.bind(request.flash.data)
      errorForm
    }
    else {
      userForm
    }

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
        Redirect(routes.UserController.login()).flashing(Flash(form.data) + ("error" -> Messages("validation.errors")))

      },
      success = {
        userFromForm =>

          if (userDao.checkUser(userFromForm)) {
            /** user form knows nothing about user id so I need get id from database */
            val temp = userDao.getUserByEmailAddress(userFromForm.email)
            UserController.mHasLoggedIn = true
            Redirect(routes.UserController.user()).withSession(
              "connected" -> userFromForm.email)

          }
          else {
            Redirect(routes.UserController.login()).flashing("error" -> "Login Failed: Please check your password or email address.")
          }
      })
  }

  def logout = Action { request =>
    UserController.mHasLoggedIn = false
    Redirect(routes.HomeController.index()).withNewSession
  }

  def user = Action { request =>
    request.session.get("connected").map { emailAddress =>
      val loginUser: User = userDao.getUserByEmailAddress(emailAddress).get
      val tempId: Long = loginUser.id.get
      Ok(views.html.User.profile(loginUser))

    }.getOrElse {
      Unauthorized("Oops, you are not connected")
    }
  }

  def profile(id: Long) = Action { request =>
    val user = userDao.findById(id)
    if (user.isEmpty) {
      Unauthorized("No such user")
    }
    else {
      request.session.get("connected").map { emailAddress =>
        val loginUser: User = userDao.getUserByEmailAddress(emailAddress).get
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
          if (userDao.isUserExisted(user.email)) Redirect(routes.UserController.signUp()).flashing(Flash(signUpForm.data) + ("error" -> Messages("User already existed")))
          else {
            userDao.signUp(user)
            Redirect(routes.UserController.login())
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


  val userInfoForm = Form(
    mapping(
      "userId" -> ignored(0: Long),
      "name" -> nonEmptyText,
      "location" -> nonEmptyText
    )(UserInfo.apply)(UserInfo.unapply)
  )

  def editUserInfo = Action { implicit request =>

    request.session.get("connected").map { emailAddress =>
      val loginUser: User = userDao.getUserByEmailAddress(emailAddress).get
      val userInfo = userDao.getUserInfo(loginUser)
      println(userInfo + "userInfo")
      println(login + "loginuser")
      val form: Form[UserInfo] = userInfoForm.fill(userInfo)
      Ok(views.html.User.userinfo(form))

    }.getOrElse {
      Redirect(routes.UserController.login())
    }
  }

  def updateUserInfo = Action { implicit request =>

    request.session.get("connected").map { emailAddress =>
      val loginUser: User = userDao.getUserByEmailAddress(emailAddress).get
      val newForm = userInfoForm.bindFromRequest()

      newForm.fold(
        hasErrors = { form =>
          println("we are having error, try to check form data is matched with html")
          println(form.data)
          Unauthorized("Oops, we are having form error")
        },
        success = {
          userInfo =>
            println("user info after success " + userInfo)
            val userInfo_ = UserInfo(loginUser.id.get,userInfo.name,userInfo.location)

            userDao.updateUserInfo(loginUser, userInfo_)
            Redirect(routes.HomeController.index())
        })

    }.getOrElse {
      Unauthorized("Oops, you are not connected")
    }
  }

  def deleteUser(user: User) = {
    userDao.deleteUser(user.email)
  }
}
