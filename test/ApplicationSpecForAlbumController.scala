//import models.{Album, Role, User}
//import utils.Silhouette._
//import com.google.inject.AbstractModule
//import net.codingwell.scalaguice.ScalaModule
//import com.mohiva.play.silhouette.api.Environment
//import com.mohiva.play.silhouette.test._
//import controllers.routes
//import dao.{AdminToolDao, AlbumDao, UserDao}
//import org.joda.time.DateTime
//import org.scalatestplus.play.guice.GuiceOneAppPerTest
//import org.scalatestplus.play.PlaySpec
//import play.api.Application
//import play.api.test.WithApplication
//import play.api.inject.guice.GuiceApplicationBuilder
//import play.api.libs.concurrent.Execution.Implicits._
//import play.api.test.Helpers.{contentAsString, _}
//import play.api.test.FakeRequest
//import play.api.i18n.Messages
////the following import is needed even though it is showing gray in IDE
//import play.api.i18n.Messages.Implicits._
//
//
///**
//  * Add your spec here.
//  * You can mock out a whole application including requests, plugins etc.
//  * For more information, consult the wiki.
//  */
//class ApplicationSpecForAlbumController extends PlaySpec with GuiceOneAppPerTest {
//
//  "Routes" should {
//    "send 404 on a bad request" in {
//      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
//    }
//
//  }
//
//  "HomeController" should {
//    "render the index page" in {
//      val home = route(app, FakeRequest(GET, "/")).get
//      status(home) mustBe OK
//      contentType(home) mustBe Some("text/html")
//      contentAsString(home) must include("This is the home page for this sample web app")
//    }
//  }
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
//  "AlbumController" should {
//
//    def getNewAlbum: Album = {
//      val artistName = "ArtistName"
//      val title = "TitleName"
//      Album(Some(1), Some(1), artistName, title)
//    }
//
//    "Part 1-user cannot add album before starting date" in new MasterUserContext {
//      new WithApplication(application) {
//        val now = DateTime.now
//        val announcementString = "announcement testing"
//        val startingDate = now.plusDays(1)
//        val endingDate = now.plusDays(2)
//        //TODO adminToolDao.makeAnnouncement(ADMIN_USER, startingDate, endingDate, announcementString)
//      }
//    }
//
//    "Part 2-user cannot add album before starting date" in new NormalUserContext {
//      new WithApplication(application) {
//        //adding part
//        val Some(addAlbumRoute) = route(app, FakeRequest(routes.AlbumController.add())
//          .withAuthenticator[MyEnv](identity.loginInfo))
//        status(addAlbumRoute) mustBe OK
//        contentAsString(addAlbumRoute) must include(Messages("album.notallowed"))
//
//        val album = getNewAlbum
//        val userId = userDao.getUserByEmailAddress(normalUser.email).get.id.get
//        albumDao.insertAlbum(album, userId)
//        val albumId = albumDao.retrieveAlbumId(album.artist, album.title, userId)
//
//        //updating part
//        val Some(editAlbumRoute) = route(app, FakeRequest(routes.AlbumController.edit(albumId.get))
//          .withAuthenticator[MyEnv](identity.loginInfo))
//        status(editAlbumRoute) mustBe OK
//        contentAsString(editAlbumRoute) must include(Messages("album.notallowed"))
//
//        //deleting part
//        val Some(deleteAlbumRoute) = route(app, FakeRequest(routes.AlbumController.delete(albumId.get))
//          .withAuthenticator[MyEnv](identity.loginInfo))
//        status(deleteAlbumRoute) mustBe OK
//        contentAsString(deleteAlbumRoute) must include(Messages("album.notallowed"))
//      }
//    }
//
//    "Part 1-user cannot add album after deadline" in new MasterUserContext {
//      new WithApplication(application) {
//        val now = DateTime.now
//        val announcementString = "announcement testing"
//        val startingDate = now.minusDays(2)
//        val endingDate = now.minusDays(1)
//        //TODO adminToolDao.makeAnnouncement(ADMIN_USER, startingDate, endingDate, announcementString)
//      }
//    }
//
//    "Part 2-user cannot add album after deadline" in new NormalUserContext {
//      new WithApplication(application) {
//        val Some(addAlbumRoute) = route(app, FakeRequest(routes.AlbumController.add())
//          .withAuthenticator[MyEnv](identity.loginInfo))
//        status(addAlbumRoute) mustBe OK
//        contentAsString(addAlbumRoute) must include(Messages("album.notallowed"))
//
//        val album = getNewAlbum
//        val userId = userDao.getUserByEmailAddress(normalUser.email).get.id.get
//        albumDao.insertAlbum(album, userId)
//        val albumId = albumDao.retrieveAlbumId(album.artist, album.title, userId)
//
//        //updating part
//        val Some(editAlbumRoute) = route(app, FakeRequest(routes.AlbumController.edit(albumId.get))
//          .withAuthenticator[MyEnv](identity.loginInfo))
//        status(editAlbumRoute) mustBe OK
//        contentAsString(editAlbumRoute) must include(Messages("album.notallowed"))
//
//        //deleting part
//        val Some(deleteAlbumRoute) = route(app, FakeRequest(routes.AlbumController.delete(albumId.get))
//          .withAuthenticator[MyEnv](identity.loginInfo))
//        status(deleteAlbumRoute) mustBe OK
//        contentAsString(deleteAlbumRoute) must include(Messages("album.notallowed"))
//      }
//    }
//
//    "Part 1-user can add album within deadline" in new MasterUserContext {
//      new WithApplication(application) {
//        val now = DateTime.now
//        val announcementString = "announcement testing"
//        val startingDate = now.minusDays(1)
//        val endingDate = now.plusDays(1)
//        //TODO adminToolDao.makeAnnouncement(ADMIN_USER, startingDate, endingDate, announcementString)
//      }
//    }
//
//    "Part 2-user can add album within deadline" in new NormalUserContext {
//      new WithApplication(application) {
//        val Some(addAlbumRoute) = route(app, FakeRequest(routes.AlbumController.add())
//          .withAuthenticator[MyEnv](identity.loginInfo))
//        status(addAlbumRoute) mustBe OK
//        contentAsString(addAlbumRoute) mustNot include(Messages("album.notallowed"))
//
//        val album = getNewAlbum
//        val userId = userDao.getUserByEmailAddress(normalUser.email).get.id.get
//        albumDao.insertAlbum(album, userId)
//        val albumId = albumDao.retrieveAlbumId(album.artist, album.title, userId)
//
//        //updating part
//        val Some(editAlbumRoute) = route(app, FakeRequest(routes.AlbumController.edit(albumId.get))
//          .withAuthenticator[MyEnv](identity.loginInfo))
//        status(editAlbumRoute) mustBe OK
//        contentAsString(editAlbumRoute) mustNot include(Messages("album.notallowed"))
//
//        //deleting part
//        val Some(deleteAlbumRoute) = route(app, FakeRequest(routes.AlbumController.delete(albumId.get))
//          .withAuthenticator[MyEnv](identity.loginInfo))
//        status(deleteAlbumRoute) mustBe SEE_OTHER
//        contentAsString(deleteAlbumRoute) mustNot include(Messages("album.notallowed"))
//      }
//    }
//
//    "normal user should able to add album" in new NormalUserContext {
//      new WithApplication(application) {
//
//        val album = getNewAlbum
//
//        val Some(addAlbumRoute) = route(app, FakeRequest(routes.AlbumController.add())
//          .withAuthenticator[MyEnv](identity.loginInfo))
//        status(addAlbumRoute) mustBe OK
//
//        val Some(saveAlbumRoute) = route(app, FakeRequest(routes.AlbumController.save())
//          .withAuthenticator[MyEnv](identity.loginInfo)
//          .withFormUrlEncodedBody("artist" -> album.artist, "title" -> album.title))
//        status(saveAlbumRoute) mustBe SEE_OTHER
//        redirectLocation(saveAlbumRoute) mustBe Some(routes.UserController.user().url)
//        userDao.deleteUser(normalUser.email)
//      }
//    }
//
//    "normal user should able to delete an album" in new NormalUserContext {
//      new WithApplication(application) {
//        val album = getNewAlbum
//        val userId = userDao.getUserByEmailAddress(normalUser.email).get.id.get
//        albumDao.insertAlbum(album, userId)
//        val albumId = albumDao.retrieveAlbumId(album.artist, album.title, userId)
//        val Some(deleteAlbumRoute) = route(app, FakeRequest(routes.AlbumController.
//          delete(albumId.get)).withAuthenticator[MyEnv](identity.loginInfo))
//        status(deleteAlbumRoute) mustBe SEE_OTHER
//        redirectLocation(deleteAlbumRoute) mustBe Some(routes.UserController.user().url)
//        albumDao.retrieveAlbumId(album.artist, album.title, userId) mustBe None
//        userDao.deleteUser(normalUser.email)
//      }
//    }
//
//    "normal user should able to edit an album" in new NormalUserContext {
//      new WithApplication(application) {
//        val album = getNewAlbum
//        val userId = userDao.getUserByEmailAddress(normalUser.email).get.id.get
//        albumDao.insertAlbum(album, userId)
//        val albumId = albumDao.retrieveAlbumId(album.artist, album.title, userId)
//
//        val Some(editAlbumRoute) = route(app, FakeRequest(routes.AlbumController.update(albumId.get))
//          .withAuthenticator[MyEnv](identity.loginInfo)
//          .withFormUrlEncodedBody("artist" -> album.artist, "title" -> album.title))
//        status(editAlbumRoute) mustBe SEE_OTHER
//        redirectLocation(editAlbumRoute) mustBe Some(routes.UserController.user().url)
//        albumDao.retrieveAlbumId(album.artist, album.title, userId) mustBe albumId
//        albumDao.delete(album, userId)
//        userDao.deleteUser(normalUser.email)
//      }
//    }
//
//    "normal user should should NOT be able to add an album when there is existing album" in new NormalUserContext {
//      new WithApplication(application) {
//        val album = getNewAlbum
//        val userId = userDao.getUserByEmailAddress(normalUser.email).get.id.get
//        albumDao.insertAlbum(album, userId)
//        val albumId = albumDao.retrieveAlbumId(album.artist, album.title, userId)
//
//        val Some(saveAlbumRoute) = route(app, FakeRequest(routes.AlbumController.save())
//          .withAuthenticator[MyEnv](identity.loginInfo)
//          .withFormUrlEncodedBody("artist" -> album.artist, "title" -> album.title))
//        status(saveAlbumRoute) mustBe SEE_OTHER
//        redirectLocation(saveAlbumRoute) mustBe Some(routes.AlbumController.add().url)
//        userDao.deleteUser(normalUser.email)
//      }
//    }
//  }
//
//  "delete normal user to clear db--this is not a test" in new NormalUserContext {
//    new WithApplication(application) {
//      userDao.deleteUser(normalUser.email)
//    }
//  }
//
//  "delete admin user to clear db--this is not a test" in new MasterUserContext {
//    new WithApplication(application) {
//      userDao.deleteUser(admin.email)
//    }
//  }
//
//  def albumDao(implicit app: Application) = {
//    val app2AlbumDAO = Application.instanceCache[AlbumDao]
//    app2AlbumDAO(app)
//  }
//
//  /**
//    * The context.
//    */
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
//    //adminToolDao.deleteAnnouncement(ADMIN_USER)
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