import models.{Album, Role, User}
import utils.Silhouette._
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.test._
import controllers.{UserController, routes}
import dao.{AlbumDao, UserDao}
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.Application
import play.api.test.WithApplication
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, _}
import play.api.test.FakeRequest

import scala.concurrent.{Future, Promise}


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

  //Ref:: https://github.com/playframework/play-slick/blob/master/samples/computer-database/test/ModelSpec.scala
  def userDao(implicit app: Application) = {
    val app2UserDAO = Application.instanceCache[UserDao]
    app2UserDAO(app)
  }

  def getNewUser(): User = {
    val emailaddress = "new@new.com"
    val password = "new"
    val username = "new"
    val services = ""
    val user = userDao.getUserByEmailAddress(emailaddress)
    if (user.isDefined) deleteNewUser(user.get)
    User(Some(1), emailaddress, password, username, Role.NormalUser, true)
  }

  def deleteNewUser(user: User) {
    userDao.deleteUser(user.email) //clean up
  }

  "UserController" should {

    "able to access login page" in {
      val loginPage = route(app, FakeRequest(routes.UserController.login())).get
      status(loginPage) mustBe OK
      contentType(loginPage) mustBe Some("text/html")
      contentAsString(loginPage) must include("Login")
    }

    "should fail to access edit page" in {
      val editPage = route(app, FakeRequest(routes.UserController.editUserInfo())).get
      status(editPage) mustBe SEE_OTHER
      redirectLocation(editPage) mustBe Some(routes.UserController.login().url)
    }

    "should able to sign up and redirect to login page" in {
      userDao.createUserTableIfNotExisted

      val user = getNewUser()

      val signUpPage = route(app, FakeRequest(routes.UserController.signUpCheck()).withFormUrlEncodedBody("email" -> user.email, "password" -> user.password, "username" -> user.username)).get
      status(signUpPage) mustBe OK
      contentAsString(signUpPage) must include("Almost Signed Up")

      deleteNewUser(user)
    }

    "should NOT be able to sign up if there is already account in DB" in {
      userDao.createUserTableIfNotExisted
      val user = getNewUser()
      val signUpPage = route(app, FakeRequest(routes.UserController.signUpCheck()).
        withFormUrlEncodedBody("email" -> user.email, "password" -> user.password, "username" -> user.username)).get
      status(signUpPage) mustBe OK

      //another attempt to sign up with same email
      val anotherSignUpPage = route(app, FakeRequest(routes.UserController.signUpCheck()).
        withFormUrlEncodedBody("email" -> user.email, "password" -> user.password, "username" -> user.username)).get
      status(anotherSignUpPage) mustBe SEE_OTHER

      val signUpFailFlashMessage = "Login Failed"
      val redirectedSignUpPage = route(app, FakeRequest(GET, redirectLocation(anotherSignUpPage).get).withFlash("error" -> signUpFailFlashMessage)).get
      status(redirectedSignUpPage) mustBe OK
      contentAsString(redirectedSignUpPage) must include("Sign Up")
      contentAsString(redirectedSignUpPage) must include(signUpFailFlashMessage)

      deleteNewUser(user)
    }

    "return 200 if user is authorized" in new MasterUserContext {
      new WithApplication(application) {
        val Some(result) = route(app, FakeRequest(routes.UserController.user())
          .withAuthenticator[MyEnv](identity.loginInfo))

        status(result) mustBe OK
        contentAsString(result) must include(masterUser.email)
      }
    }

    "should re-route to user page when user is already logged in and trying to access login page " in new MasterUserContext {
      new WithApplication(application) {
        val Some(tryingAccessLoginPage: Future[Result]) = route(app, FakeRequest(routes.UserController.login())
          .withAuthenticator[MyEnv](identity.loginInfo))

        tryingAccessLoginPage.map {
          _ =>
            status(tryingAccessLoginPage) mustBe SEE_OTHER
            redirectLocation(tryingAccessLoginPage) mustBe Some(routes.UserController.user().url)
        }.recover {
          case _ => println("we have future error which only happened when on server")
        }

        val userToBeDeleted = userDao.getUserByEmailAddress(masterUser.email)
        if (userToBeDeleted.isDefined) userDao.deleteUser(userToBeDeleted.get.email)
      }
    }

    "master user should access to master page" in new MasterUserContext {
      new WithApplication(application) {
        val Some(tryingAccessLoginPage) = route(app, FakeRequest(routes.UserController.master())
          .withAuthenticator[MyEnv](identity.loginInfo))
        status(tryingAccessLoginPage) mustBe OK

        val userToBeDeleted = userDao.getUserByEmailAddress(masterUser.email)
        if (userToBeDeleted.isDefined) userDao.deleteUser(userToBeDeleted.get.email)
      }
    }

    "normal user should not access to master page" in new NormalUserContext {
      new WithApplication(application) {
        val Some(tryingAccessLoginPage) = route(app, FakeRequest(routes.UserController.master())
          .withAuthenticator[MyEnv](identity.loginInfo))
        status(tryingAccessLoginPage) mustBe UNAUTHORIZED

        val userToBeDeleted = userDao.getUserByEmailAddress(normalUser.email)
        if (userToBeDeleted.isDefined) userDao.deleteUser(userToBeDeleted.get.email)
      }
    }

    "should NOT be able to login and redirect to login page when there is no user" in {
      userDao.createUserTableIfNotExisted
      val emailAddress = "nosuchuser@nosuchuser.com"
      val password = "nosuchuser"
      val username = "name"
      val user = userDao.getUserByEmailAddress(emailAddress)
      user.isEmpty mustBe true

      val loginPage = route(app, FakeRequest(routes.UserController.loginCheck()).
        withFormUrlEncodedBody("email" -> emailAddress, "password" -> password, "username" -> username)).get
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

    "normal user can apply to be master" in new NormalUserContext {
      new WithApplication(application) {
        val Some(result) = route(app, FakeRequest(routes.UserController.requestToBeMaster())
          .withAuthenticator[MyEnv](identity.loginInfo))
        status(result) mustBe OK

        val userToBeDeleted = userDao.getUserByEmailAddress(normalUser.email)
        if (userToBeDeleted.isDefined) userDao.deleteUser(userToBeDeleted.get.email)
      }
    }
    "part 1 - normal user can become master using token" in new NormalUserContext {
      new WithApplication(application) {

        val Some(result) = route(app, FakeRequest(routes.UserController.requestToBeMaster())
          .withAuthenticator[MyEnv](identity.loginInfo))
        status(result) mustBe OK

        val Some(approveWithToken: Future[Result]) = route(app, FakeRequest(routes.UserController.approveUserWithToken(UserController.getToken)).withAuthenticator[MyEnv](identity.loginInfo))
        status(approveWithToken) mustBe OK
      }
    }

    "part 2 - normal user can become master using token" in {
      //Note: role in part 1 does not reflect immediately//No idea
      //That's why, we are testing the role again in this test
      val role = userDao.getUserByEmailAddress(NORMAL_USER_EMAIL).get.role
      role mustBe Role.Admin
      userDao.deleteUser(NORMAL_USER_EMAIL)
    }

  }

  "AlbumController" should {

    def getNewAlbum: Album = {
      val artistName = "ArtistName"
      val title = "TitleName"
      Album(Some(1), Some(1), artistName, title)
    }

    "normal user should able to add album" in new NormalUserContext {
      new WithApplication(application) {

        val album = getNewAlbum

        val Some(addAlbumRoute) = route(app, FakeRequest(routes.AlbumController.add())
          .withAuthenticator[MyEnv](identity.loginInfo))
        status(addAlbumRoute) mustBe OK

        val Some(saveAlbumRoute) = route(app, FakeRequest(routes.AlbumController.save())
          .withAuthenticator[MyEnv](identity.loginInfo)
          .withFormUrlEncodedBody("artist" -> album.artist, "title" -> album.title))
        status(saveAlbumRoute) mustBe SEE_OTHER
        redirectLocation(saveAlbumRoute) mustBe Some(routes.UserController.user().url)
        userDao.deleteUser(normalUser.email)
      }
    }

    "normal user should able to delete an album" in new NormalUserContext {
      new WithApplication(application) {
        val album = getNewAlbum
        val userId = userDao.getUserByEmailAddress(normalUser.email).get.id.get
        albumDao.insertAlbum(album, userId)
        val albumId = albumDao.retrieveAlbumId(album.artist, album.title, userId)
        val Some(deleteAlbumRoute) = route(app, FakeRequest(routes.AlbumController.
          delete(albumId.get)).withAuthenticator[MyEnv](identity.loginInfo))
        status(deleteAlbumRoute) mustBe SEE_OTHER
        redirectLocation(deleteAlbumRoute) mustBe Some(routes.UserController.user().url)
        albumDao.retrieveAlbumId(album.artist, album.title, userId) mustBe None
        userDao.deleteUser(normalUser.email)
      }
    }

    "normal user should able to edit an album" in new NormalUserContext {
      new WithApplication(application) {
        val album = getNewAlbum
        val userId = userDao.getUserByEmailAddress(normalUser.email).get.id.get
        albumDao.insertAlbum(album, userId)
        val albumId = albumDao.retrieveAlbumId(album.artist, album.title, userId)

        val Some(editAlbumRoute) = route(app, FakeRequest(routes.AlbumController.update(albumId.get))
          .withAuthenticator[MyEnv](identity.loginInfo)
          .withFormUrlEncodedBody("artist" -> album.artist, "title" -> album.title))
        status(editAlbumRoute) mustBe SEE_OTHER
        redirectLocation(editAlbumRoute) mustBe Some(routes.UserController.user().url)
        albumDao.retrieveAlbumId(album.artist, album.title, userId) mustBe albumId
        albumDao.delete(album, userId)
        userDao.deleteUser(normalUser.email)
      }
    }

    "normal user should should NOT be able to add an album when there is existing album" in new NormalUserContext {
      new WithApplication(application) {
        val album = getNewAlbum
        val userId = userDao.getUserByEmailAddress(normalUser.email).get.id.get
        albumDao.insertAlbum(album, userId)
        val albumId = albumDao.retrieveAlbumId(album.artist, album.title, userId)

        val Some(saveAlbumRoute) = route(app, FakeRequest(routes.AlbumController.save())
          .withAuthenticator[MyEnv](identity.loginInfo)
          .withFormUrlEncodedBody("artist" -> album.artist, "title" -> album.title))
        status(saveAlbumRoute) mustBe SEE_OTHER
        redirectLocation(saveAlbumRoute) mustBe Some(routes.AlbumController.add().url)
        userDao.deleteUser(normalUser.email)
      }
    }
  }

  def albumDao(implicit app: Application) = {
    val app2AlbumDAO = Application.instanceCache[AlbumDao]
    app2AlbumDAO(app)
  }

  /**
    * The context.
    */
  trait MasterUserContext {

    class FakeModule extends AbstractModule with ScalaModule {
      def configure() = {
        bind[Environment[MyEnv]].toInstance(env)
      }
    }

    val emailAddress = "abc@abc.com"
    val masterUser = User(Some(1), emailAddress, "password", "username", Role.Admin, true)

    userDao.insertUserWithHashPassword(masterUser)
    val identity = userDao.getUserByEmailAddress(masterUser.email).get

    implicit val env: Environment[MyEnv] = new FakeEnvironment[MyEnv](Seq(identity.loginInfo -> identity))

    lazy val application = new GuiceApplicationBuilder().overrides(new FakeModule()).build
  }

  val NORMAL_USER_EMAIL = "xyz@xyz.com"

  trait NormalUserContext {

    class FakeModule extends AbstractModule with ScalaModule {
      def configure() = {
        bind[Environment[MyEnv]].toInstance(env)
      }
    }

    val normalUser = User(Some(1), NORMAL_USER_EMAIL, "password", "username", Role.NormalUser, true)
    userDao.deleteUser(normalUser.email)
    userDao.insertUserWithHashPassword(normalUser)
    val identity = userDao.getUserByEmailAddress(normalUser.email).get

    implicit val env: Environment[MyEnv] = new FakeEnvironment[MyEnv](Seq(identity.loginInfo -> identity))

    lazy val application = new GuiceApplicationBuilder().overrides(new FakeModule()).build
  }

}

