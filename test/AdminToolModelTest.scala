
/**
  * Created by s43132 on 4/4/2017.
  */

import dao.{AdminToolDao, UserDao}
import models.{Role, User}
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import org.specs2.mutable.Specification
import play.api.Application
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class AdminToolModelTest extends PlaySpec with BeforeAndAfterEach with GuiceOneAppPerSuite {

  def adminToolDao(implicit app: Application) = {
    val app2AdminToolDAO = Application.instanceCache[AdminToolDao]
    app2AdminToolDAO(app)
  }

  def userDao(implicit app: Application) = {
    val app2UserDAO = Application.instanceCache[UserDao]
    app2UserDAO(app)
  }

  private val ADMIN_EMAIL = "admin@admin.com"

  override def beforeEach(): Unit = {
    userDao.addUser(getMasterUser)
  }

  def getMasterUser: User = {
    User(Some(1), ADMIN_EMAIL, "just email", "admin", Role.Admin, true)
  }

  "Admin Tool Model" should {

    "should create announcement" in {
      val user = Await.result(userDao.getUserByEmail(getMasterUser.email), Duration.Inf)
      val announcement = "This is an announcement"
      val firstAdminTool = Await.result(adminToolDao.getAdminTool, Duration.Inf)
      val startingDate = DateTime.now()
      val endingDate = DateTime.now()
      Await.result(adminToolDao.createAnnouncement(user.get, firstAdminTool.get,
        announcement, startingDate, endingDate), Duration.Inf)

      val secondAdminTool = Await.result(adminToolDao.getAdminTool, Duration.Inf).get

      secondAdminTool.announcement.get.compareTo(announcement) mustBe 0
      secondAdminTool.startingDate.get.compareTo(startingDate) mustBe 0
      secondAdminTool.endingDate.get.compareTo(endingDate) mustBe 0
    }

  }

  override def afterEach(): Unit = {
    val user = Await.result(userDao.getUserByEmail(getMasterUser.email), Duration.Inf).get
    val adminTool = Await.result(adminToolDao.getAdminTool, Duration.Inf).get
    Await.result(adminToolDao.deleteAnnouncement(user, adminTool), Duration.Inf)
    userDao.deleteUser(ADMIN_EMAIL)
  }
}
