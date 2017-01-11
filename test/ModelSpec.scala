/**
  * Created by aknay on 6/1/17.
  */

import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import dao.UserDao
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec


class ModelSpec extends PlaySpec with  BeforeAndAfterAll with OneAppPerSuite{

  import models._

  def userDao(implicit app: Application) = {
    val app2UserDAO = Application.instanceCache[UserDao]
    app2UserDAO(app)
  }

  val EMAIL_NAME = "qwe@qwe.com"

  override def beforeAll(): Unit = {
    userDao.createUserTableIfNotExisted
    userDao.createUserInfoTableIfNotExisted
  }

  "User Model" should {
    "User Dao" should {
      "insert data and check its existance" in {
        userDao.createUserTableIfNotExisted
        userDao.createUserInfoTableIfNotExisted
        val user = User(Some(99), EMAIL_NAME, "qwe")
        userDao.insertUserWithUserInfo(user) mustBe true
      }
    }
  }

  override def afterAll(): Unit = {
    userDao.deleteUser(EMAIL_NAME)
  }
}