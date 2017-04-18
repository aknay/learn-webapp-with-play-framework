package ModelTests

/**
  * Created by aknay on 4/4/17.
  */

import dao.{AdminToolDao, UserDao}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite


class UserModelTest extends PlaySpec with BeforeAndAfterEach with GuiceOneAppPerSuite with ScalaFutures {

  import models._

  private val ADMIN_EMAIL = "user@user.com"

  def getMasterUser: User = {
    User(Some(1), ADMIN_EMAIL, "just email", "admin", Role.Admin, activated = true)
  }

  val userDao: UserDao = app.injector.instanceOf(classOf[UserDao])
  val adminToolDao: AdminToolDao = app.injector.instanceOf(classOf[AdminToolDao])


  "should add user" in {
    userDao.deleteUserByEmail(ADMIN_EMAIL).futureValue
    val result = userDao.insertUser(getMasterUser).futureValue
    result mustBe true
  }

  "should remove user" in {
    //setup
    userDao.insertUser(getMasterUser).futureValue
    userDao.deleteUserByEmail(ADMIN_EMAIL).futureValue
    val result = userDao.getUserByEmail(ADMIN_EMAIL).futureValue
    //test
    result mustBe None
  }
}

