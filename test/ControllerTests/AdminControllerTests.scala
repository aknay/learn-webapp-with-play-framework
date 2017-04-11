package ControllerTests

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.test._
import controllers.routes
import dao.{AdminToolDao, UserDao}
import models.{Role, User}
import net.codingwell.scalaguice.ScalaModule
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.Execution.Implicits._
import play.api.test.Helpers.{contentAsString, _}
import play.api.test.{FakeRequest, WithApplication}
import utils.Silhouette._

import scala.concurrent.duration._
import scala.concurrent.Await
//the following import is needed even though it is showing gray in IDE
import play.api.i18n.Messages.Implicits._
import scala.concurrent.Future


class AdminControllerTests extends PlaySpec with GuiceOneAppPerTest with ScalaFutures {

  //Ref:: https://github.com/playframework/play-slick/blob/master/samples/computer-database/test/ModelSpec.scala
  def userDao(implicit app: Application) = {
    val app2UserDAO = Application.instanceCache[UserDao]
    app2UserDAO(app)
  }

  def adminToolDao(implicit app: Application) = {
    val app2AdminToolDAO = Application.instanceCache[AdminToolDao]
    app2AdminToolDAO(app)
  }

  def await[T](fut: Future[T]): T = Await.result(fut, Duration.Inf)

  def deleteNewUser(user: User) {
    userDao.removeUser(user.email) //clean up
  }

  "Admin Controller" should {

    "admin user should access to admin page" in new MasterUserContext {
      new WithApplication(application) {
        val Some(tryingAccessLoginPage) = route(app, FakeRequest(routes.AdminController.admin())
          .withAuthenticator[MyEnv](ADMIN_USER.loginInfo))
        status(tryingAccessLoginPage) mustBe OK
      }
    }

    "only master user can make announcement" in new MasterUserContext {
      new WithApplication(application) {
        //setup
        val Some(view) = route(app, FakeRequest(routes.AdminController.viewAnnouncementForm()).withAuthenticator[MyEnv](ADMIN_USER.loginInfo))
        //test
        status(view) mustBe OK
        //setup
        //form only accepts this format
        val startingDate = "10-10-2010"
        val endingDate = "11-10-2010"
        //this will be displayed to user
        val startingDateAsMonthFormat = "10-Oct-2010"
        val endingDateAsMonthFormat = "11-Oct-2010"
        val announcement = "test"
        val announcementPage = route(app, FakeRequest(routes.AdminController.submitAnnouncement()).
          withFormUrlEncodedBody("startingDate" -> startingDate, "endingDate" -> endingDate, "announcement" -> announcement).withAuthenticator[MyEnv](ADMIN_USER.loginInfo)).get
        status(announcementPage) mustBe SEE_OTHER
        redirectLocation(announcementPage) mustBe Some(routes.AdminController.viewSuccessfulAnnouncement().url)
        val Some(redirectedPage) = route(app, FakeRequest(routes.AdminController.viewSuccessfulAnnouncement()).withAuthenticator[MyEnv](ADMIN_USER.loginInfo))
        contentAsString(redirectedPage) must include(Messages("announcement.successful", announcement, startingDateAsMonthFormat, endingDateAsMonthFormat))
        val startingDateNow = adminToolDao.getStartingDate

        //Ref: http://stackoverflow.com/questions/8202546/joda-invalid-format-exception
        //we need to convert string to date
        val formattedStatingDate: DateTime = adminToolDao.getFormattedDate(startingDateAsMonthFormat)
        val startingDateFromFuture = await(adminToolDao.getStartingDate)
        startingDateFromFuture.get.get.compareTo(formattedStatingDate) mustBe 0

        val endingDateNow = adminToolDao.getEndingDate
        val formattedEndingDate: DateTime = adminToolDao.getFormattedDate(endingDateAsMonthFormat)
        val endingDateFromFuture = await(adminToolDao.getEndingDate)
        endingDateFromFuture.get.get.compareTo(formattedEndingDate) mustBe 0
      }
    }

    "master see empty announcement when there is none" in new MasterUserContext {
      new WithApplication(application) {
        //setup
        await(adminToolDao.deleteAnnouncement(ADMIN_USER))
        //test
        val Some(viewAnnouncement) = route(app, FakeRequest(routes.AdminController.viewAnnouncement())
          .withAuthenticator[MyEnv](ADMIN_USER.loginInfo))
        status(viewAnnouncement) mustBe OK
        contentAsString(viewAnnouncement) must include(Messages("announcement.empty"))
      }
    }

    "master user can view announcement after it has been created" in new MasterUserContext {
      new WithApplication(application) {
        //setup
        val startingDateString = "10-Oct-2010"
        val endingDateString = "11-Oct-2010"
        val announcementString = "announcement testing"
        val startingDate = adminToolDao.getFormattedDate(startingDateString)
        val endingDate = adminToolDao.getFormattedDate(endingDateString)
        await(adminToolDao.createAnnouncement(ADMIN_USER, announcementString, startingDate, endingDate))
        //test
        val Some(announcementPage) = route(app, FakeRequest(routes.AdminController.viewAnnouncement())
          .withAuthenticator[MyEnv](ADMIN_USER.loginInfo))
        status(announcementPage) mustBe OK
        contentAsString(announcementPage) must include(startingDateString)
        contentAsString(announcementPage) must include(endingDateString)
        contentAsString(announcementPage) must include(announcementString)
      }
    }
    "user should see announcement on home page" in new MasterUserContext {
      new WithApplication(application) {
        //setup
        val startingDateString = "10-Oct-2010"
        val endingDateString = "11-Oct-2010"
        val announcementString = "announcement testing"
        val startingDate = adminToolDao.getFormattedDate(startingDateString)
        val endingDate = adminToolDao.getFormattedDate(endingDateString)
        await(adminToolDao.createAnnouncement(ADMIN_USER, announcementString, startingDate, endingDate))
        //test
        val mainPage = route(app, FakeRequest(routes.HomeController.index())).get
        status(mainPage) mustBe OK
        contentAsString(mainPage) must include(Messages("announcement.intro"))
        contentAsString(mainPage) must include(announcementString)
      }
    }


    "master user cannot see announcement on profile page when there is none" in new MasterUserContext {
      new WithApplication(application) {
        //setup
        await(adminToolDao.deleteAnnouncement(ADMIN_USER))
        val Some(viewAnnouncement) = route(app, FakeRequest(routes.AdminController.admin())
          .withAuthenticator[MyEnv](ADMIN_USER.loginInfo))
        //test
        status(viewAnnouncement) mustBe OK
        contentAsString(viewAnnouncement) mustNot include(Messages("admin.view.announcement"))
        contentAsString(viewAnnouncement) mustNot include(Messages("admin.edit.announcement"))
        contentAsString(viewAnnouncement) mustNot include(Messages("admin.delete.announcement"))
        contentAsString(viewAnnouncement) must include(Messages("admin.make.announcement"))
      }
    }
    //
    "master user can see announcement on profile page when there is one" in new MasterUserContext {
      new WithApplication(application) {
        //setup
        val startingDateString = "10-Oct-2010"
        val endingDateString = "11-Oct-2010"
        val announcementString = "announcement testing"
        val startingDate = adminToolDao.getFormattedDate(startingDateString)
        val endingDate = adminToolDao.getFormattedDate(endingDateString)
        await(adminToolDao.createAnnouncement(ADMIN_USER, announcementString, startingDate, endingDate))
        val Some(viewAnnouncement) = route(app, FakeRequest(routes.AdminController.admin())
          .withAuthenticator[MyEnv](ADMIN_USER.loginInfo))
        //test
        adminToolDao.getAdminTool must not be None
        status(viewAnnouncement) mustBe OK
        contentAsString(viewAnnouncement) must include(Messages("admin.view.announcement"))
        contentAsString(viewAnnouncement) must include(Messages("admin.edit.announcement"))
        contentAsString(viewAnnouncement) must include(Messages("admin.delete.announcement"))
      }
    }

    "delete admin user to clear db--this is not a test" in new MasterUserContext {
      new WithApplication(application) {
        userDao.removeUser(ADMIN_EMAIL)
      }
    }

  }

  trait MasterUserContext {

    class FakeModule extends AbstractModule with ScalaModule {
      def configure() = {
        bind[Environment[MyEnv]].toInstance(env)
      }
    }

    val ADMIN_EMAIL = "abc@abc.com"
    val admin = User(Some(1), ADMIN_EMAIL, "password", "username", Role.Admin, true)
    userDao.insertUser(admin).futureValue
    val ADMIN_USER: User = userDao.getUserByEmail(ADMIN_EMAIL).futureValue.get
    implicit val env: Environment[MyEnv] = new FakeEnvironment[MyEnv](Seq(ADMIN_USER.loginInfo -> ADMIN_USER))
    lazy val application = new GuiceApplicationBuilder().overrides(new FakeModule()).build
  }

}
