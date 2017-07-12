package ModelTests

/**
  * Created by aknay on 4/4/17.
  */

import dao.UserDao
import play.api.Application
import play.api.test.{PlaySpecification, WithApplication}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class UserDaoTest extends PlaySpecification {

  import models._

  private val NORMAL_USER_EMAIL = "dummy_normal@user.com"

  def getNormalUser: User = {
    User(Some(1), NORMAL_USER_EMAIL, "just email", "admin", Role.NormalUser, activated = true)
  }

  def userDao(implicit app: Application) = Application.instanceCache[UserDao].apply(app)

  "should add user" in new WithApplication() {
    await(userDao.deleteUserByEmail(NORMAL_USER_EMAIL))

    val result = await(userDao.insertUser(getNormalUser))
    result === true
    val user = await(userDao.getUserByEmail(NORMAL_USER_EMAIL))
    user.isDefined === true
  }

  "should remove user" in new WithApplication() {
    await(userDao.deleteUserByEmail(NORMAL_USER_EMAIL))

    val isInserted = await(userDao.insertUser(getNormalUser))
    isInserted === true
    await(userDao.deleteUserByEmail(NORMAL_USER_EMAIL))
    val result = await(userDao.getUserByEmail(NORMAL_USER_EMAIL))
    result === None
  }

  "should also add user info when user is added" in new WithApplication() {
    await(userDao.deleteUserByEmail(NORMAL_USER_EMAIL))

    await(userDao.insertUser(getNormalUser))
    val insertedUser = await(userDao.getUserByEmail(NORMAL_USER_EMAIL))
    val userInfo = await(userDao.getUserInfo(insertedUser.get))
    userInfo.isDefined === true
  }

  "should get user info" in new WithApplication() {
    await(userDao.deleteUserByEmail(NORMAL_USER_EMAIL))

    await(userDao.insertUser(getNormalUser))
    val insertedUser = await(userDao.getUserByEmail(NORMAL_USER_EMAIL))
    val userInfo = await(userDao.getUserInfo(insertedUser.get))
    userInfo.isDefined === true
    userInfo.get.name === "EMPTY"
    userInfo.get.location === "EMPTY"
  }

  "should modified user info" in new WithApplication() {
    await(userDao.deleteUserByEmail(NORMAL_USER_EMAIL))

    await(userDao.insertUser(getNormalUser))
    val insertedUser = await(userDao.getUserByEmail(NORMAL_USER_EMAIL))
    val userInfo = await(userDao.getUserInfo(insertedUser.get))
    await(userDao.updateUserInfo(insertedUser.get, "User", "planet"))
    val updatedUserInfo = await(userDao.getUserInfo(insertedUser.get))
    updatedUserInfo.get.name === "User"
    updatedUserInfo.get.location === "planet"
  }

  "clean up" in new WithApplication() {
    await(userDao.deleteUserByEmail(NORMAL_USER_EMAIL))
  }

  def await[T](v: Future[T]): T = Await.result(v, Duration.Inf)
}

