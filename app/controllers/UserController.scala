package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{Clock, Credentials, PasswordHasherRegistry}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, Flash}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.data.Forms._
import com.mohiva.play.silhouette.api.{LoginEvent, LoginInfo, LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import utils.Silhouette.Implicits._
import dao.{AlbumDao, UserDao}
import models.{User, UserInfo}
import forms.SignUpForm
import play.api.Configuration
import utils.Silhouette.{AuthController, MyEnv, UserService}

import scala.concurrent.duration.FiniteDuration


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
                               credentialsProvider: CredentialsProvider,
                               conf: Configuration,
                               clock: Clock,
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

  def loginCheck = UnsecuredAction.async { implicit request =>
    val loginForm = SignUpForm.form.bindFromRequest()
    loginForm.fold(
      formWithErrors => Future.successful(BadRequest(views.html.User.login(SignUpForm.form))),
      formData => {
        credentialsProvider.authenticate(Credentials(formData.email, formData.password)).flatMap { loginInfo =>
          userService.retrieve(loginInfo).flatMap {
            case Some(user) => for {
              authenticator <- env.authenticatorService.create(loginInfo)
              cookie <- env.authenticatorService.init(authenticator)
              result <- env.authenticatorService.embed(cookie, Redirect(routes.UserController.user()))
            } yield {
              env.eventBus.publish(LoginEvent(user, request))
              result
            }
            case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
          }
        }.recover {
          case e: ProviderException => Redirect(routes.UserController.login()).flashing("error" -> Messages("auth.credentials.incorrect"))
        }
      }
    )
  }

  def logout = SecuredAction.async { implicit request =>
    env.eventBus.publish(LogoutEvent(request.identity, request))
    env.authenticatorService.discard(request.authenticator, Redirect(routes.HomeController.index()))
  }

  def user(page: Int) = SecuredAction.async { request =>
    val loginUser: User = userDao.getUserByEmailAddress(request.identity.email).get
    val tempId: Long = loginUser.id.get
    albumDao.listWithPage(tempId, page = page).map {
      page => Ok(views.html.User.profile(request.identity, page))
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
