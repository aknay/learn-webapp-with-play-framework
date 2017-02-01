package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, Flash}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.data.Forms._
import com.mohiva.play.silhouette.api.{LoginInfo, Silhouette}
import utils.Silhouette.Implicits._
import dao.{AlbumDao, UserDao}
import models.{User, UserInfo}
import forms.SignUpForm
import utils.Silhouette.{AuthController, MyEnv, UserService}


/**
  * Created by aknay on 27/12/16.
  */

object UserController {
  private var mHasLoggedIn = false;

  def hasLoggedIn: Boolean = mHasLoggedIn
}


class UserController @Inject()(userDao: UserDao,
                               albumDao: AlbumDao,
                               albumController: AlbumController,
                               userService: UserService,
                               authInfoRepository: AuthInfoRepository,
                               passwordHasherRegistry: PasswordHasherRegistry)
                              (val messagesApi: MessagesApi,
                               val silhouette: Silhouette[MyEnv])
  extends AuthController with I18nSupport {

  def login = Action { implicit request =>
    val form = if (request.flash.get("error").isDefined) {
      val errorForm = SignUpForm.form.bind(request.flash.data)
      errorForm
    }
    else {
      SignUpForm.form
    }

    Ok(views.html.User.login(SignUpForm.form))

  }

  def signUp = Action { implicit request =>
    Ok(views.html.User.signup(SignUpForm.form))
  }

  def signUpCheck = UnsecuredAction.async { implicit request =>
    SignUpForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.User.signup(form))),
      user => {
        val loginInfo: LoginInfo = user.email
        userService.retrieve(loginInfo).flatMap {
          case Some(_) => Future.successful(Redirect(routes.UserController.signUp()).flashing(Flash(SignUpForm.form.data) + ("error" -> Messages("User already existed")))) //user is not unique
          case None => {
            for {
              savedUser <- userService.save(user)
              _ <- authInfoRepository.add(loginInfo, passwordHasherRegistry.current.hash(user.password))
            } yield {
              Redirect(routes.UserController.login()) //TODO: //Ok(views.html.User.signupsuccess(savedUser))
            }
          }
        }
      }
    )
  }

  def loginCheck = Action { implicit request =>
    val loginForm = SignUpForm.form.bindFromRequest()
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

  def user(page: Int) = Action.async { request =>
    request.session.get("connected").map { emailAddress =>
      val loginUser: User = userDao.getUserByEmailAddress(emailAddress).get
      val tempId: Long = loginUser.id.get
      albumDao.listWithPage(tempId, page = page).map {
        page => Ok(views.html.User.profile(loginUser, page))
      }

    }.getOrElse {
      Future.successful(Unauthorized("Oops, you are not connected"))
    }
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
            val userInfo_ = UserInfo(loginUser.id.get, userInfo.name, userInfo.location)

            userDao.updateUserInfo(loginUser, userInfo_)
            Redirect(routes.HomeController.index())
        })

    }.getOrElse {
      Unauthorized("Oops, you are not connected")
    }
  }

  def deleteUser(user: User): Int = {
    userDao.deleteUser(user.email)
  }


}
