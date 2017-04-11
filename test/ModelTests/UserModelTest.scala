package ModelTests

/**
  * Created by aknay on 4/4/17.
  */

import dao.{AdminToolDao, UserDao}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.test.WithApplication

class UserModelTest extends PlaySpec with BeforeAndAfterEach with GuiceOneAppPerSuite with ScalaFutures {

  import models._

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
      userDao.removeUser(ADMIN_EMAIL).futureValue
      val result = userDao.insertUser(getMasterUser).futureValue
      result mustBe true
    }

    "should remove user" in new WithApplication {
      //setup
      userDao.insertUser(getMasterUser).futureValue
      userDao.removeUser(ADMIN_EMAIL).futureValue
      val result = userDao.getUserByEmail(ADMIN_EMAIL).futureValue
      //test
      result mustBe None
    }
  }

}