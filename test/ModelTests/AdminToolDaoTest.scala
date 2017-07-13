package ModelTests

/**
  * Created by aknay on 4/4/17.
  */

import dao.{AdminToolDao, UserDao}
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.specs2.mutable.Specification
import play.api.Application
import play.api.test.WithApplication


class AdminToolDaoTest extends Specification with ScalaFutures {

  import models._

  private val NORMAL_USER_EMAIL = "dummy_normal@user.com"

  def getNormalUser = User(Some(1), NORMAL_USER_EMAIL, "password", "username", Role.NormalUser, activated = true)

  def getAdminUser = User(Some(2), "admin@user.com", "password", "username", Role.Admin, activated = true)


  def userDao(implicit app: Application) = Application.instanceCache[UserDao].apply(app)

  def adminToolDao(implicit app: Application) = Application.instanceCache[AdminToolDao].apply(app)

  "should create announcement" in new WithApplication() {
    userDao.insertUser(getAdminUser).futureValue
    val announcement = "This is an AAA announcement"
    val firstAdminTool = adminToolDao.getAdminTool.futureValue
    firstAdminTool.isDefined === true
    val user = userDao.getUserByEmail(getAdminUser.email).futureValue
    adminToolDao.createAnnouncement(user.get, announcement, DateTime.now(), DateTime.now()).futureValue
    adminToolDao.getAdminTool.futureValue.get.announcement.get === announcement
  }

}

