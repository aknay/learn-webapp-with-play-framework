/**
  * Created by aknay on 4/4/17.
  */


import dao.{AdminToolDao, UserDao}
import org.joda.time.DateTime
import play.api.Application

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import org.specs2.mutable.Specification

import play.api.test.WithApplication

class AdminToolModelTest extends Specification {

  import models._

  def await[T](fut: Future[T]) = Await.result(fut, Duration.Inf)
  
  private val ADMIN_EMAIL = "admin@admin.com"

  def getMasterUser: User = {
    User(Some(1), ADMIN_EMAIL, "just email", "admin", Role.Admin, activated = true)
  }

  "Computer model" should {

    def adminToolDao(implicit app: Application): AdminToolDao = {
      val app2AdminToolDAO = Application.instanceCache[AdminToolDao]
      app2AdminToolDAO(app)
    }

    def userDao(implicit app: Application): UserDao = {
      val app2UserDAO = Application.instanceCache[UserDao]
      app2UserDAO(app)
    }

    "be retrieved by id" in new WithApplication {
      await(userDao.removeUser(ADMIN_EMAIL))
      //      userDao.addUser(getMasterUser)
      val result = await(userDao.addUser(getMasterUser))
      result must equalTo(true)

    }

    "should create an announement" in new WithApplication {
      await(userDao.removeUser(ADMIN_EMAIL))
      val result = await(userDao.addUser(getMasterUser))
      result must equalTo(true)

      Thread.sleep(100) //TODO this is not right
      val user = await(userDao.getUserByEmail(ADMIN_EMAIL))
      Thread.sleep(100)
      user.isDefined must equalTo(true)
      val announcement = "This is an announcement"
      val firstAdminTool = await(adminToolDao.getAdminTool)
      firstAdminTool.isDefined must equalTo(true)

      val startingDate = DateTime.now()
      val endingDate = DateTime.now()
      await(adminToolDao.createAnnouncement(user.get, firstAdminTool.get,
        announcement, startingDate, endingDate))

      val secondAdminTool = await(adminToolDao.getAdminTool).get

      secondAdminTool.announcement.get.compareTo(announcement) must equalTo(0)
      secondAdminTool.startingDate.get.compareTo(startingDate) must equalTo(0)
      secondAdminTool.endingDate.get.compareTo(endingDate) must equalTo(0)

      await(userDao.removeUser(ADMIN_EMAIL))
    }
  }

}