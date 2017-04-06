package ModelTests

/**
  * Created by aknay on 4/4/17.
  */


import dao.{AdminToolDao, UserDao}
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import play.api.Application
import play.api.test.WithApplication

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class AdminToolModelTest extends Specification {

  import models._

  def await[T](fut: Future[T]): T = Await.result(fut, Duration.Inf)

  private val ADMIN_EMAIL = "justAdmin@admin.com"

  def getMasterUser: User = {
    User(Some(1), ADMIN_EMAIL, "just email", "admin", Role.Admin, activated = true)
  }


  "AdminTool model" should {

    def adminToolDao(implicit app: Application): AdminToolDao = {
      val app2AdminToolDAO = Application.instanceCache[AdminToolDao]
      app2AdminToolDAO(app)
    }

    def userDao(implicit app: Application): UserDao = {
      val app2UserDAO = Application.instanceCache[UserDao]
      app2UserDAO(app)
    }

    def getUser(implicit app: Application): User = {
      val user = userDao.getUserByEmailWithBlocking(ADMIN_EMAIL)
      if (user.isEmpty) {
        val u = User(Some(1), ADMIN_EMAIL, "just email", "admin", Role.Admin, activated = true)
        userDao.addUserWithBlocking(User(Some(1), ADMIN_EMAIL, "just email", "admin", Role.Admin, activated = true))
        return userDao.getUserByEmailWithBlocking(ADMIN_EMAIL).get
      }
      user.get
    }

    "preparing and setup" in new WithApplication {
      await(userDao.removeUser(ADMIN_EMAIL))
      val result = await(userDao.addUser(getMasterUser))
      result must equalTo(true)

      Thread.sleep(100)
      //TODO this is not right
      val user = await(userDao.getUserByEmail(ADMIN_EMAIL))
      Thread.sleep(100)
      user.isDefined must equalTo(true)
    }


    "should create an announcement" in new WithApplication {
      val announcement = "This is an announcement"
      val startingDate = DateTime.now()
      val endingDate = DateTime.now()
      await(adminToolDao.createAnnouncement(getUser, announcement, startingDate, endingDate))

      val secondAdminTool = await(adminToolDao.getAdminTool).get

      secondAdminTool.announcement.get.compareTo(announcement) must equalTo(0)
      secondAdminTool.startingDate.get.compareTo(startingDate) must equalTo(0)
      secondAdminTool.endingDate.get.compareTo(endingDate) must equalTo(0)
    }


    "should delete announcement" in new WithApplication {
      val announcement = "This is an announcement"
      val startingDate = DateTime.now()
      val endingDate = DateTime.now()
      await(adminToolDao.createAnnouncement(getUser, announcement, startingDate, endingDate))

      val secondAdminTool = await(adminToolDao.getAdminTool).get

      secondAdminTool.announcement.get.compareTo(announcement) must equalTo(0)
      secondAdminTool.startingDate.get.compareTo(startingDate) must equalTo(0)
      secondAdminTool.endingDate.get.compareTo(endingDate) must equalTo(0)

      await(adminToolDao.deleteAnnouncement(getUser))

      val adminTool = adminToolDao.getAdminToolWithBlocking
      adminTool.get.announcement must equalTo(None)
      adminTool.get.startingDate must equalTo(None)
      adminTool.get.endingDate must equalTo(None)

    }

    "should add event" in new WithApplication {
      val eventName = "abc event"
      await(adminToolDao.addEvent(getUser, eventName)) must equalTo(true)
    }

    "should get event" in new WithApplication {
      val eventName = "abc event"

      val t: Future[Option[String]] = adminToolDao.getEvent.map{
        case Some(a) => a
        case None => None
      }

      await(t).isDefined must equalTo(true)
    }





  }

}