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

    def removeAllEvents(implicit app: Application) = {
      val adminTool = adminToolDao.getAdminToolWithBlocking.get
      await(adminToolDao.updateAdminTool(getUser, adminTool))
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
      removeAllEvents
      val firstEvent = "abc event"

      await(adminToolDao.addEvent(getUser, firstEvent)) must equalTo(true)
      Thread.sleep(1000)
      await(adminToolDao.getEvent).get.get.compareTo(firstEvent) must equalTo(0)

      //add additional event
      val secondEvent = "def event"
      await(adminToolDao.addEvent(getUser, secondEvent)) must equalTo(true)
      val allEvents = firstEvent + "," + secondEvent
      println("allEvents: " + allEvents)
      println("get it from: " + await(adminToolDao.getEvent).get.get)
      await(adminToolDao.getEvent).get.get.compareTo(allEvents) must equalTo(0)
    }

    "should delete an event" in new WithApplication {
      removeAllEvents
      val firstEvent = "abc event"
      val secondEvent = "def event"
      await(adminToolDao.addEvent(getUser, firstEvent))
      await(adminToolDao.addEvent(getUser, secondEvent))

      val isDeleted = await(adminToolDao.deleteEvent(getUser, firstEvent))
      isDeleted must equalTo(true)
      await(adminToolDao.getEvent).get.isDefined must equalTo(true)

      val isDeletedAgain = await(adminToolDao.deleteEvent(getUser, secondEvent))
      isDeletedAgain must equalTo(true)
      await(adminToolDao.getEvent).get.isDefined must equalTo(false)
    }
  }

}