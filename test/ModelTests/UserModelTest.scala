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

class UserModelTest extends Specification {

  import models._

  def await[T](fut: Future[T]): T = Await.result(fut, Duration.Inf)

  private val ADMIN_EMAIL = "user@user.com"

  def getMasterUser: User = {
    User(Some(1), ADMIN_EMAIL, "just email", "admin", Role.Admin, activated = true)
  }

  "User model" should {

    def adminToolDao(implicit app: Application): AdminToolDao = {
      val app2AdminToolDAO = Application.instanceCache[AdminToolDao]
      app2AdminToolDAO(app)
    }

    def userDao(implicit app: Application): UserDao = {
      val app2UserDAO = Application.instanceCache[UserDao]
      app2UserDAO(app)
    }

    "should add user" in new WithApplication {
      await(userDao.removeUser(ADMIN_EMAIL))
      val result = await(userDao.addUser(getMasterUser))
      result must equalTo(true)
    }

    "should remove user" in new WithApplication {
      //setup
      await(userDao.addUser(getMasterUser))
      await(userDao.removeUser(ADMIN_EMAIL))
      val result = await(userDao.getUserByEmail(ADMIN_EMAIL))
      //test
      result must equalTo(None)
    }




  }

}