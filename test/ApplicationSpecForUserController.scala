//import com.google.inject.AbstractModule
//import com.mohiva.play.silhouette.api.Environment
//import com.mohiva.play.silhouette.test._
//import controllers.{UserController, routes}
//import dao.{AdminToolDao, AlbumDao, UserDao}
//import models.{Album, Role, User}
//import net.codingwell.scalaguice.ScalaModule
//import org.joda.time.DateTime
//import org.scalatestplus.play.PlaySpec
//import org.scalatestplus.play.guice.GuiceOneAppPerTest
//import play.api.Application
//import play.api.i18n.Messages
//import play.api.inject.guice.GuiceApplicationBuilder
//import play.api.libs.concurrent.Execution.Implicits._
//import play.api.mvc.Result
//import play.api.test.Helpers.{contentAsString, _}
//import play.api.test.{FakeRequest, WithApplication}
//import utils.Silhouette._
//import scala.concurrent.duration._
//
//import scala.concurrent.Await
////the following import is needed even though it is showing gray in IDE
//import play.api.i18n.Messages.Implicits._
//import scala.concurrent.Future
//
//
///**
//  * Add your spec here.
//  * You can mock out a whole application including requests, plugins etc.
//  * For more information, consult the wiki.
//  */
//class ApplicationSpecForUserController extends PlaySpec with GuiceOneAppPerTest {
//
//  //Ref:: https://github.com/playframework/play-slick/blob/master/samples/computer-database/test/ModelSpec.scala
//  def userDao(implicit app: Application) = {
//    val app2UserDAO = Application.instanceCache[UserDao]
//    app2UserDAO(app)
//  }
//
//  def adminToolDao(implicit app: Application) = {
//    val app2AdminToolDAO = Application.instanceCache[AdminToolDao]
//    app2AdminToolDAO(app)
//  }
//
//  def getNewUser(): User = {
//    val emailaddress = "new@new.com"
//    val password = "new"
//    val username = "new"
//    val services = ""
//    val user = userDao.getUserByEmailAddress(emailaddress)
//    if (user.isDefined) deleteNewUser(user.get)
//    User(Some(1), emailaddress, password, username, Role.NormalUser, true)
//  }
//
//  def deleteNewUser(user: User) {
//    userDao.deleteUser(user.email) //clean up
//  }
//
//  "UserController" should {
//    "able to access login page" in {
//      val loginPage = route(app, FakeRequest(routes.UserController.login())).get
//      status(loginPage) mustBe OK
//      contentType(loginPage) mustBe Some("text/html")
//      contentAsString(loginPage) must include("Login")
//    }
//
//    "should fail to access edit page when user has not logged in" in {
//      val editPage = route(app, FakeRequest(routes.UserController.editUserInfo())).get
//      status(editPage) mustBe UNAUTHORIZED
//    }
//
//    "should  access edit page after user has logged in" in new NormalUserContext {
//      new WithApplication(application) {
//        val Some(result) = route(app, FakeRequest(routes.UserController.editUserInfo())
//          .withAuthenticator[MyEnv](normalUser.loginInfo))
//        status(result) mustBe OK
//        contentAsString(result) must include(Messages("settings.profile.title"))
//      }
//    }
//
//    "should update user info after user has logged in" in new NormalUserContext {
//      new WithApplication(application) {
//        val Some(editUserInfoPage) = route(app, FakeRequest(routes.UserController.updateUserInfo())
//          .withAuthenticator[MyEnv](identity.loginInfo)
//          .withFormUrlEncodedBody("name" -> "username", "location" -> "planet"))
//        status(editUserInfoPage) mustBe SEE_OTHER
//        redirectLocation(editUserInfoPage) mustBe Some(routes.UserController.editUserInfo().url)
//        val Some(result) = route(app, FakeRequest(routes.UserController.editUserInfo())
//          .withAuthenticator[MyEnv](normalUser.loginInfo))
//        status(result) mustBe OK
//        contentAsString(result) must include("planet")
//        contentAsString(result) must include("username")
//      }
//    }
//
//    "should able to sign up and redirect to login page" in {
//      userDao.createUserTableIfNotExisted
//
//      val user = getNewUser()
//
//      val signUpPage = route(app, FakeRequest(routes.UserController.signUpCheck()).withFormUrlEncodedBody("email" -> user.email, "password" -> user.password, "username" -> user.username)).get
//      status(signUpPage) mustBe OK
//      contentAsString(signUpPage) must include("Almost Signed Up")
//
//      deleteNewUser(user)
//    }
//
//    "should NOT be able to sign up if there is already account in DB" in {
//      userDao.createUserTableIfNotExisted
//      val user = getNewUser()
//      val signUpPage = route(app, FakeRequest(routes.UserController.signUpCheck()).
//        withFormUrlEncodedBody("email" -> user.email, "password" -> user.password, "username" -> user.username)).get
//      status(signUpPage) mustBe OK
//
//      //another attempt to sign up with same email
//      val anotherSignUpPage = route(app, FakeRequest(routes.UserController.signUpCheck()).
//        withFormUrlEncodedBody("email" -> user.email, "password" -> user.password, "username" -> user.username)).get
//      status(anotherSignUpPage) mustBe SEE_OTHER
//
//      val signUpFailFlashMessage = "Login Failed"
//      val redirectedSignUpPage = route(app, FakeRequest(GET, redirectLocation(anotherSignUpPage).get).withFlash("error" -> signUpFailFlashMessage)).get
//      status(redirectedSignUpPage) mustBe OK
//      contentAsString(redirectedSignUpPage) must include("Sign Up")
//      contentAsString(redirectedSignUpPage) must include(signUpFailFlashMessage)
//
//      deleteNewUser(user)
//    }
//
//    "return 200 if user is authorized" in new MasterUserContext {
//      new WithApplication(application) {
//        val Some(result) = route(app, FakeRequest(routes.UserController.user())
//          .withAuthenticator[MyEnv](ADMIN_USER.loginInfo))
//
//        status(result) mustBe OK
//        contentAsString(result) must include(admin.email)
//      }
//    }
//
//    "should re-route to user page when user is already logged in and trying to access login page " in new MasterUserContext {
//      new WithApplication(application) {
//        val Some(tryingAccessLoginPage: Future[Result]) = route(app, FakeRequest(routes.UserController.login())
//          .withAuthenticator[MyEnv](ADMIN_USER.loginInfo))
//
//        tryingAccessLoginPage.map {
//          _ =>
//            status(tryingAccessLoginPage) mustBe SEE_OTHER
//            redirectLocation(tryingAccessLoginPage) mustBe Some(routes.UserController.user().url)
//        }.recover {
//          case _ => println("we have future error which only happened when on server")
//        }
//
//        val userToBeDeleted = userDao.getUserByEmailAddress(admin.email)
//        if (userToBeDeleted.isDefined) userDao.deleteUser(userToBeDeleted.get.email)
//      }
//    }
//
//    "master user should access to master page" in new MasterUserContext {
//      new WithApplication(application) {
//        val Some(tryingAccessLoginPage) = route(app, FakeRequest(routes.AdminController.admin())
//          .withAuthenticator[MyEnv](ADMIN_USER.loginInfo))
//        status(tryingAccessLoginPage) mustBe OK
//
//        val userToBeDeleted = userDao.getUserByEmailAddress(admin.email)
//        if (userToBeDeleted.isDefined) userDao.deleteUser(userToBeDeleted.get.email)
//      }
//    }
//
//
//    "only master user can make announcement" in new MasterUserContext {
//      new WithApplication(application) {
//
//        val Some(view) = route(app, FakeRequest(routes.AdminController.viewAnnouncementForm()).withAuthenticator[MyEnv](ADMIN_USER.loginInfo))
//        status(view) mustBe OK
//        //form only accepts this format
//        val startingDate = "10-10-2010"
//        val endingDate = "11-10-2010"
//        //this will be displayed to user
//        val startingDateAsMonthFormat = "10-Oct-2010"
//        val endingDateAsMonthFormat = "11-Oct-2010"
//        val announcement = "test"
//        val announcementPage = route(app, FakeRequest(routes.AdminController.announcementCheck()).
//          withFormUrlEncodedBody("startingDate" -> startingDate, "endingDate" -> endingDate, "announcement" -> announcement).withAuthenticator[MyEnv](ADMIN_USER.loginInfo)).get
//        status(announcementPage) mustBe SEE_OTHER
//        redirectLocation(announcementPage) mustBe Some(routes.AdminController.viewSuccessfulAnnouncement().url)
//        val Some(redirectedPage) = route(app, FakeRequest(routes.AdminController.viewSuccessfulAnnouncement()).withAuthenticator[MyEnv](ADMIN_USER.loginInfo))
//        contentAsString(redirectedPage) must include(Messages("announcement.successful", announcement, startingDateAsMonthFormat, endingDateAsMonthFormat))
//        val startingDateNow = adminToolDao.getStartingDate
//
//        //Ref: http://stackoverflow.com/questions/8202546/joda-invalid-format-exception
//        //we need to convert string to date
//        val formattedStatingDate: DateTime = adminToolDao.getFormattedDate(startingDateAsMonthFormat)
////        startingDateNow.get.compareTo(formattedStatingDate) mustBe 0
//        val startingDateFromFuture = Await.result(adminToolDao.getStartingDate, 2 seconds)
//          startingDateFromFuture.get.get.compareTo(formattedStatingDate) mustBe 0
//
//        val endingDateNow = adminToolDao.getEndingDate
//        val formattedEndingDate: DateTime = adminToolDao.getFormattedDate(endingDateAsMonthFormat)
//        val endingDateFromFuture = Await.result(adminToolDao.getEndingDate, 2 seconds)
//        endingDateFromFuture.get.get.compareTo(formattedEndingDate) mustBe 0
//        //endingDateNow.get.compareTo(formattedEndingDate) mustBe 0
//      }
//    }
//
//    "master user can view announcement" in new MasterUserContext {
//      new WithApplication(application) {
//
//        val Some(viewAnnouncement) = route(app, FakeRequest(routes.AdminController.viewAnnouncement())
//          .withAuthenticator[MyEnv](ADMIN_USER.loginInfo))
//        status(viewAnnouncement) mustBe OK
//        contentAsString(viewAnnouncement) must include(Messages("announcement.empty"))
//
//        val startingDateString = "10-Oct-2010"
//        val endingDateString = "11-Oct-2010"
//        val announcementString = "announcement testing"
//        val startingDate = adminToolDao.getFormattedDate(startingDateString)
//        val endingDate = adminToolDao.getFormattedDate(endingDateString)
//        //TODO adminToolDao.makeAnnouncement(ADMIN_USER, startingDate, endingDate, announcementString)
//
//        val Some(announcementPage) = route(app, FakeRequest(routes.AdminController.viewAnnouncement())
//          .withAuthenticator[MyEnv](ADMIN_USER.loginInfo))
//        status(announcementPage) mustBe OK
//        contentAsString(announcementPage) must include(startingDateString)
//        contentAsString(announcementPage) must include(endingDateString)
//        contentAsString(announcementPage) must include(announcementString)
//      }
//    }
//
//    "master user cannot see announcement when there is none" in new MasterUserContext {
//      new WithApplication(application) {
//        val Some(viewAnnouncement) = route(app, FakeRequest(routes.AdminController.admin())
//          .withAuthenticator[MyEnv](ADMIN_USER.loginInfo))
//       //TODO adminToolDao.deleteAnnouncement(ADMIN_USER)
//        //        adminToolDao.getAdminTool mustBe None
//        status(viewAnnouncement) mustBe OK
//        contentAsString(viewAnnouncement) mustNot include(Messages("admin.view.announcement"))
//        contentAsString(viewAnnouncement) mustNot include(Messages("admin.edit.announcement"))
//        contentAsString(viewAnnouncement) mustNot include(Messages("admin.delete.announcement"))
//        contentAsString(viewAnnouncement) must include(Messages("admin.make.announcement"))
//      }
//    }
//
//    "master user can see announcement when there is one" in new MasterUserContext {
//      new WithApplication(application) {
//
//        val startingDateString = "10-Oct-2010"
//        val endingDateString = "11-Oct-2010"
//        val announcementString = "announcement testing"
//        val startingDate = adminToolDao.getFormattedDate(startingDateString)
//        val endingDate = adminToolDao.getFormattedDate(endingDateString)
//        //TODO adminToolDao.makeAnnouncement(ADMIN_USER, startingDate, endingDate, announcementString)
//        val Some(viewAnnouncement) = route(app, FakeRequest(routes.AdminController.admin())
//          .withAuthenticator[MyEnv](ADMIN_USER.loginInfo))
//        adminToolDao.getAdminTool must not be None
//        status(viewAnnouncement) mustBe OK
//        contentAsString(viewAnnouncement) must include(Messages("admin.view.announcement"))
//        contentAsString(viewAnnouncement) must include(Messages("admin.edit.announcement"))
//        contentAsString(viewAnnouncement) must include(Messages("admin.delete.announcement"))
//      }
//    }
//
//    "user should not see any announcement on main page when there is none" in new MasterUserContext {
//      new WithApplication(application) {
//        val Some(mainPage) = route(app, FakeRequest(routes.HomeController.index()))
//        status(mainPage) mustBe OK
//        contentAsString(mainPage) mustNot include(Messages("announcement.intro"))
//      }
//    }
//
//    "user should see announcement on main page" in new MasterUserContext {
//      new WithApplication(application) {
//        val startingDateString = "10-Oct-2010"
//        val endingDateString = "11-Oct-2010"
//        val announcementString = "announcement testing"
//        val startingDate = adminToolDao.getFormattedDate(startingDateString)
//        val endingDate = adminToolDao.getFormattedDate(endingDateString)
//        // TODO adminToolDao.makeAnnouncement(ADMIN_USER, startingDate, endingDate, announcementString)
//        val Some(mainPage) = route(app, FakeRequest(routes.HomeController.index()))
//        status(mainPage) mustBe OK
//        contentAsString(mainPage) must include(Messages("announcement.intro"))
//        contentAsString(mainPage) must include(announcementString)
//      }
//    }
//
//    "normal user should not access to master page" in new NormalUserContext {
//      new WithApplication(application) {
//        val Some(tryingAccessLoginPage) = route(app, FakeRequest(routes.AdminController.admin())
//          .withAuthenticator[MyEnv](identity.loginInfo))
//        status(tryingAccessLoginPage) mustBe UNAUTHORIZED
//
//        val userToBeDeleted = userDao.getUserByEmailAddress(normalUser.email)
//        if (userToBeDeleted.isDefined) userDao.deleteUser(userToBeDeleted.get.email)
//      }
//    }
//
//    "should NOT be able to login and redirect to login page when there is no user" in {
//      userDao.createUserTableIfNotExisted
//      val emailAddress = "nosuchuser@nosuchuser.com"
//      val password = "nosuchuser"
//      val username = "name"
//      val user = userDao.getUserByEmailAddress(emailAddress)
//      user.isEmpty mustBe true
//
//      val loginPage = route(app, FakeRequest(routes.UserController.loginCheck()).
//        withFormUrlEncodedBody("email" -> emailAddress, "password" -> password, "username" -> username)).get
//      status(loginPage) mustBe SEE_OTHER
//      redirectLocation(loginPage) mustBe Some(routes.UserController.login().url)
//
//      //after redirect
//      val loginFailFlashMessage = "Login Failed"
//      val redirectedLoginPage = route(app, FakeRequest(GET, redirectLocation(loginPage).get).withFlash("error" -> loginFailFlashMessage)).get
//      status(redirectedLoginPage) mustBe OK
//      contentAsString(redirectedLoginPage) must include("Login")
//      contentAsString(redirectedLoginPage) must include(loginFailFlashMessage)
//      if (user.isDefined) userDao.deleteUser(user.get.email) //clean up
//    }
//
//    "normal user can apply to be master" in new NormalUserContext {
//      new WithApplication(application) {
//        val Some(result) = route(app, FakeRequest(routes.UserController.requestToBeMaster())
//          .withAuthenticator[MyEnv](identity.loginInfo))
//        status(result) mustBe OK
//
//        val userToBeDeleted = userDao.getUserByEmailAddress(normalUser.email)
//        if (userToBeDeleted.isDefined) userDao.deleteUser(userToBeDeleted.get.email)
//      }
//    }
//
//    "part 1 - normal user can become master using token" in new NormalUserContext {
//      new WithApplication(application) {
//
//        val Some(result) = route(app, FakeRequest(routes.UserController.requestToBeMaster())
//          .withAuthenticator[MyEnv](identity.loginInfo))
//        status(result) mustBe OK
//
//        val Some(approveWithToken: Future[Result]) = route(app, FakeRequest(routes.UserController.approveUserWithToken(UserController.getToken)).withAuthenticator[MyEnv](identity.loginInfo))
//        status(approveWithToken) mustBe OK
//      }
//    }
//
//    "part 2 - normal user can become master using token" in {
//      //Note: role in part 1 does not reflect immediately//No idea
//      //That's why, we are testing the role again in this test
//      val role = userDao.getUserByEmailAddress(NORMAL_USER_EMAIL).get.role
//      role mustBe Role.Admin
//      userDao.deleteUser(NORMAL_USER_EMAIL)
//    }
//
//    "logged in user should redirect to Home page if reset password page is accessed" in new NormalUserContext {
//      new WithApplication(application) {
//        val Some(result) = route(app, FakeRequest(routes.UserController.requestResetPassword())
//          .withAuthenticator[MyEnv](identity.loginInfo))
//        status(result) mustBe SEE_OTHER
//        redirectLocation(result) mustBe Some(routes.HomeController.index().url)
//      }
//    }
//
//    "user should see reset password page if haven't log in yet" in new NormalUserContext {
//      new WithApplication(application) {
//        val Some(result) = route(app, FakeRequest(routes.UserController.requestResetPassword()))
//        status(result) mustBe OK
//      }
//    }
//
//    "user should reset the password" in new NormalUserContext {
//      new WithApplication(application) {
//        val forgetPasswordForm = route(app, FakeRequest(routes.UserController.handleForgotPassword()).
//          withFormUrlEncodedBody("email" -> normalUser.email)).get
//        status(forgetPasswordForm) mustBe OK
//        val token = UserController.getToken
//        val Some(resetPassword) = route(app, FakeRequest(routes.UserController.resetPassword(token)))
//        status(resetPassword) mustBe OK
//        val failResetPasswordForm = route(app, FakeRequest(routes.UserController.handleResetPassword(token))
//          .withFormUrlEncodedBody("password1" -> "abcdef", "password2" -> "abcdefg")).get
//        status(failResetPasswordForm) mustBe SEE_OTHER
//        val successResetPasswordForm = route(app, FakeRequest(routes.UserController.handleResetPassword(token))
//          .withFormUrlEncodedBody("password1" -> "abcdefg", "password2" -> "abcdefg")).get
//        status(successResetPasswordForm) mustBe OK
//      }
//    }
//    "delete normal user to clear db--this is not a test" in new NormalUserContext {
//      new WithApplication(application) {
//        userDao.deleteUser(normalUser.email)
//      }
//    }
//
//    "delete admin user to clear db--this is not a test" in new MasterUserContext {
//      new WithApplication(application) {
//        userDao.deleteUser(admin.email)
//      }
//    }
//  }
//
//  trait MasterUserContext {
//
//    class FakeModule extends AbstractModule with ScalaModule {
//      def configure() = {
//        bind[Environment[MyEnv]].toInstance(env)
//      }
//    }
//
//    val ADMIN_EMAIL = "abc@abc.com"
//    val admin = User(Some(1), ADMIN_EMAIL, "password", "username", Role.Admin, true)
//    userDao.deleteUser(admin.email)
//    userDao.insertUserWithHashPassword(admin)
//    val ADMIN_USER = userDao.getUserByEmailAddress(admin.email).get
//    //TODO adminToolDao.deleteAnnouncement(ADMIN_USER)
//
//    implicit val env: Environment[MyEnv] = new FakeEnvironment[MyEnv](Seq(ADMIN_USER.loginInfo -> ADMIN_USER))
//
//    lazy val application = new GuiceApplicationBuilder().overrides(new FakeModule()).build
//  }
//
//  val NORMAL_USER_EMAIL = "xyz@xyz.com"
//
//  trait NormalUserContext {
//
//    class FakeModule extends AbstractModule with ScalaModule {
//      def configure() = {
//        bind[Environment[MyEnv]].toInstance(env)
//      }
//    }
//
//    val normalUser = User(Some(1), NORMAL_USER_EMAIL, "password", "username", Role.NormalUser, true)
//    userDao.deleteUser(normalUser.email)
//    userDao.insertUserWithHashPassword(normalUser)
//    val identity = userDao.getUserByEmailAddress(normalUser.email).get
//
//    implicit val env: Environment[MyEnv] = new FakeEnvironment[MyEnv](Seq(identity.loginInfo -> identity))
//
//    lazy val application = new GuiceApplicationBuilder().overrides(new FakeModule()).build
//  }
//
//}
