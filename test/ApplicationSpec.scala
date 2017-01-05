import controllers.routes
import dao.UserDao
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers.{contentAsString, contentType, _}
import play.api.Application

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApplicationSpec extends PlaySpec with OneAppPerTest {

  "Routes" should {

    "send 404 on a bad request" in  {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

  }

  "HomeController" should {

    "render the index page" in {
      val home = route(app, FakeRequest(GET, "/")).get
      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include ("This is the home page for this sample web app")
    }
  }

    "UserController" should {
    "able to access login page" in {
      val loginPage = route(app, FakeRequest(routes.UserController.login())).get
      status(loginPage) mustBe OK
      contentType(loginPage) mustBe Some("text/html")
      contentAsString(loginPage) must include ("Login")
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
    "should able to login" in {
      userDao.createTableIfNotExisted
      val emailAddress = "qaz@qaz.com"
      val password = "qaz"
      val user = userDao.findByEmailAddress(emailAddress)
      if (user.isDefined) userDao.deleteUser(user.get)
      val editPage = route(app, FakeRequest(routes.UserController.signUpCheck()).withFormUrlEncodedBody(("email",emailAddress),("password",password))).get
      status(editPage) mustBe SEE_OTHER
      redirectLocation(editPage) mustBe Some(routes.UserController.login().url)
    }
  }

}
