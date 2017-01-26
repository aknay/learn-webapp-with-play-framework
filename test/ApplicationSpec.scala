import controllers.routes
import dao.{AlbumDao, UserDao}
import models.{Album, Page, User}
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers.{contentAsString, contentType, _}
import play.api.Application

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Add your spec here.
  * You can mock out a whole application including requests, plugins etc.
  * For more information, consult the wiki.
  */
class ApplicationSpec extends PlaySpec with OneAppPerTest {

  "Routes" should {

    "send 404 on a bad request" in {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

  }

  "HomeController" should {

    "render the index page" in {
      val home = route(app, FakeRequest(GET, "/")).get
      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include("This is the home page for this sample web app")
    }
  }

  "UserController" should {
    "able to access login page" in {
      val loginPage = route(app, FakeRequest(routes.UserController.login())).get
      status(loginPage) mustBe OK
      contentType(loginPage) mustBe Some("text/html")
      contentAsString(loginPage) must include("Login")
    }
  }

  "UserController" should {
    "should fail to access edit page" in {
      val editPage = route(app, FakeRequest(routes.UserController.editUserInfo())).get
      status(editPage) mustBe SEE_OTHER
      redirectLocation(editPage) mustBe Some(routes.UserController.login().url)
    }
  }

  //Ref:: https://github.com/playframework/play-slick/blob/master/samples/computer-database/test/ModelSpec.scala
  def userDao(implicit app: Application) = {
    val app2UserDAO = Application.instanceCache[UserDao]
    app2UserDAO(app)
  }

  "UserController" should {
    "should able to sign up and redirect to login page" in {
      userDao.createUserTableIfNotExisted
      val emailAddress = "qaz@qaz.com"
      val password = "qaz"
      val user = userDao.getUserByEmailAddress(emailAddress)
      if (user.isDefined) userDao.deleteUser(user.get.email)
      val signUpPage = route(app, FakeRequest(routes.UserController.signUpCheck()).withFormUrlEncodedBody("email" -> emailAddress, "password" -> password)).get
      status(signUpPage) mustBe SEE_OTHER
      redirectLocation(signUpPage) mustBe Some(routes.UserController.login().url)

      //after redirect
      val loginPage = route(app, FakeRequest(GET, redirectLocation(signUpPage).get)).get
      status(loginPage) mustBe OK
      contentAsString(loginPage) must include("Login")

      if (user.isDefined) userDao.deleteUser(user.get.email) //clean up

    }
  }

  "UserController" should {
    "should NOT be able to sign up if there is already account in DB" in {
      userDao.createUserTableIfNotExisted
      val emailAddress = "qaz@qaz.com"
      val password = "qaz"
      val user = userDao.getUserByEmailAddress(emailAddress)
      if (user.isDefined) userDao.deleteUser(user.get.email)
      val signUpPage = route(app, FakeRequest(routes.UserController.signUpCheck()).withFormUrlEncodedBody("email" -> emailAddress, "password" -> password)).get
      status(signUpPage) mustBe SEE_OTHER
      redirectLocation(signUpPage) mustBe Some(routes.UserController.login().url)

      //another attempt to sing up with same email
      val anotherSignUpPage = route(app, FakeRequest(routes.UserController.signUpCheck())
        .withFormUrlEncodedBody("email" -> emailAddress, "password" -> password)).get
      status(anotherSignUpPage) mustBe SEE_OTHER

      val signUpFailFlashMessage = "Login Failed"
      val redirectedSignUpPage = route(app, FakeRequest(GET, redirectLocation(anotherSignUpPage).get).withFlash("error" -> signUpFailFlashMessage)).get
      status(redirectedSignUpPage) mustBe OK
      contentAsString(redirectedSignUpPage) must include("Sign Up")
      contentAsString(redirectedSignUpPage) must include(signUpFailFlashMessage)
      if (user.isDefined) userDao.deleteUser(user.get.email) //clean up

    }
  }

  "UserController" should {
    "should able to login and redirect to login page" in {
      userDao.createUserTableIfNotExisted

      val emailAddress = "abc@abc.com"
      val password = "abc"

      if (userDao.isUserExisted(emailAddress)) userDao.deleteUser(emailAddress)

      val signUpPage = route(app, FakeRequest(routes.UserController.signUpCheck()).withFormUrlEncodedBody("email" -> emailAddress, "password" -> password)).get
      status(signUpPage) mustBe SEE_OTHER
      redirectLocation(signUpPage) mustBe Some(routes.UserController.login().url)


      val loginPage = route(app, FakeRequest(routes.UserController.loginCheck()).
        withFormUrlEncodedBody("email" -> emailAddress, "password" -> password)).get
      status(loginPage) mustBe SEE_OTHER
      redirectLocation(loginPage) mustBe Some(routes.UserController.user().url)

      //after redirect
      val userPage = route(app, FakeRequest(GET, redirectLocation(loginPage).get).withSession("connected" -> emailAddress)).get
      status(userPage) mustBe OK
      contentAsString(userPage) must include(emailAddress)

      val user = userDao.getUserByEmailAddress(emailAddress)

      if (user.isDefined) userDao.deleteUser(user.get.email) //clean
    }
  }

  "UserController" should {
    "should NOT be able to login and redirect to login page when there is no user" in {
      userDao.createUserTableIfNotExisted
      val emailAddress = "nosuchuser@nosuchuser.com"
      val password = "nosuchuser"
      val user = userDao.getUserByEmailAddress(emailAddress)
      user.isEmpty mustBe true

      val loginPage = route(app, FakeRequest(routes.UserController.loginCheck()).
        withFormUrlEncodedBody(("email", emailAddress), ("password", password))).get
      status(loginPage) mustBe SEE_OTHER
      redirectLocation(loginPage) mustBe Some(routes.UserController.login().url)

      //after redirect
      val loginFailFlashMessage = "Login Failed"
      val redirectedLoginPage = route(app, FakeRequest(GET, redirectLocation(loginPage).get).withFlash("error" -> loginFailFlashMessage)).get
      status(redirectedLoginPage) mustBe OK
      contentAsString(redirectedLoginPage) must include("Login")
      contentAsString(redirectedLoginPage) must include(loginFailFlashMessage)
      if (user.isDefined) userDao.deleteUser(user.get.email) //clean up
    }
  }

  val ARTIST_NAME = "artist"
  val TITLE_NAME = "title"
  val EMAIL_ADDRESS = "abc@abc.com"
  val PASSWORD = "abc"
  val ANOTHER_TITLE = "another title"

  "AlbumController" should {
    "should be able to add an album" in {
      userDao.createUserTableIfNotExisted


      if (userDao.isUserExisted(EMAIL_ADDRESS)) userDao.deleteUser(EMAIL_ADDRESS)
      //sign up
      val signUpPage = route(app, FakeRequest(routes.UserController.signUpCheck()).withFormUrlEncodedBody("email" -> EMAIL_ADDRESS, "password" -> PASSWORD)).get
      status(signUpPage) mustBe SEE_OTHER
      redirectLocation(signUpPage) mustBe Some(routes.UserController.login().url)

      //login
      val loginPage = route(app, FakeRequest(routes.UserController.loginCheck()).
        withFormUrlEncodedBody("email" -> EMAIL_ADDRESS, "password" -> PASSWORD)).get
      status(loginPage) mustBe SEE_OTHER
      redirectLocation(loginPage) mustBe Some(routes.UserController.user().url)

      //actual test
      val addAlbumRoute = route(app, FakeRequest(routes.AlbumController.add())).get
      status(addAlbumRoute) mustBe OK

      val saveAlbumRoute = route(app, FakeRequest(routes.AlbumController.save()).withSession("connected" -> EMAIL_ADDRESS)
        .withFormUrlEncodedBody("artist" -> ARTIST_NAME, "title" -> TITLE_NAME)).get
      status(saveAlbumRoute) mustBe SEE_OTHER
      redirectLocation(saveAlbumRoute) mustBe Some(routes.UserController.user().url)
    }
  }

  "AlbumController" should {
    "should NOT be able to add an album when there is existing album" in {
      val saveAlbumRoute = route(app, FakeRequest(routes.AlbumController.save()).withSession("connected" -> EMAIL_ADDRESS)
        .withFormUrlEncodedBody("artist" -> ARTIST_NAME, "title" -> TITLE_NAME)).get
      status(saveAlbumRoute) mustBe SEE_OTHER
      redirectLocation(saveAlbumRoute) mustBe Some(routes.AlbumController.add().url)
    }
  }

  def albumDao(implicit app: Application) = {
    val app2AlbumDAO = Application.instanceCache[AlbumDao]
    app2AlbumDAO(app)
  }

  "AlbumController" should {
    "should be able to edit an album" in {
      val userId = userDao.getUserByEmailAddress(EMAIL_ADDRESS).get.id.get
      val albumId = albumDao.retrieveAlbumId(ARTIST_NAME, TITLE_NAME, userId)
      val editAlbumRoute = route(app, FakeRequest(routes.AlbumController.update(albumId.get)).withSession("connected" -> EMAIL_ADDRESS)
        .withFormUrlEncodedBody("artist" -> ARTIST_NAME, "title" -> ANOTHER_TITLE)).get
      status(editAlbumRoute) mustBe SEE_OTHER
      redirectLocation(editAlbumRoute) mustBe Some(routes.UserController.user().url)
      albumDao.retrieveAlbumId(ARTIST_NAME, ANOTHER_TITLE, userId) mustBe albumId
    }
  }

  
  "AlbumController" should {
    "should be able to delete an album" in {
      val userId = userDao.getUserByEmailAddress(EMAIL_ADDRESS).get.id.get
      val albumId = albumDao.retrieveAlbumId(ARTIST_NAME, ANOTHER_TITLE, userId)
      val deleteAlbumRoute = route(app, FakeRequest(routes.AlbumController.
        delete(albumId.get)).withSession("connected" -> EMAIL_ADDRESS)).get
      status(deleteAlbumRoute) mustBe SEE_OTHER
      redirectLocation(deleteAlbumRoute) mustBe Some(routes.UserController.user().url)
      albumDao.retrieveAlbumId(ARTIST_NAME, ANOTHER_TITLE, userId) mustBe None
    }
  }





}
