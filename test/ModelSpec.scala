/**
  * Created by aknay on 6/1/17.
  */

import org.scalatestplus.play.OneAppPerTest
import play.api.Application

import dao.UserDao
import org.scalatestplus.play.PlaySpec

class ModelSpec extends PlaySpec with OneAppPerTest{

  import models._


  "User Model" should {

    def userDao(implicit app: Application) = {
      val app2UserDAO = Application.instanceCache[UserDao]
      app2UserDAO(app)
    }

    "User Dao" should {

      "insert data and check its existance" in {
        val email = "qwe@qwe.com"
        val user = User(Some(1),"qwe@qwe.com", "qwe")
        userDao.insertUserWithUserInfo(user) mustBe true
        userDao.isUserExisted(email) mustBe true

        userDao.deleteUser(user) //clean up
      }
    }

  }

}