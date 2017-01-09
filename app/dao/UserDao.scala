package dao

import javax.inject.Inject
import javax.inject.Singleton

import models.{User, UserInfo}
import org.mindrot.jbcrypt.BCrypt
import slick.jdbc.meta.MTable

import scala.concurrent._
import scala.concurrent.duration._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by aknay on 27/12/16.
  */
/** change to traits so that other dao can access this user dao */
/** Ref:https://github.com/playframework/play-slick/blob/master/samples/computer-database/app/dao/CompaniesDAO.scala */
trait UsersComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>
  private val TABLE_NAME = "usertable"

  import driver.api._

  /** Since we are using album id as Option[Long], so we need to use id.? */
  class UserTable(tag: Tag) extends Table[User](tag, TABLE_NAME) {

    def email = column[String]("email")

    def password = column[String]("password")

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def * = (id.?, email, password) <> (User.tupled, User.unapply)

  }

}


@Singleton()
class UserDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends UsersComponent with HasDatabaseConfigProvider[JdbcProfile] {

  /** describe the structure of the tables: */
  /** Note: table cannot be named as 'user', otherwise we will problem with Postgresql */

  import driver.api._

  //  //TableQuery value which represents the actual database table
  private lazy val userTable = TableQuery[UserTable]


  /** The following statements are Action */
  private lazy val createTableAction = userTable.schema.create

  private val selectAlbumAction = userTable.result

  /** Ref: http://slick.lightbend.com/doc/3.0.0/database.html */

  //This is the blocking method with maximum waiting time of 2 seconds
  //This is also helper method for DBIO
  private def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)

  def getUserTable: Future[Seq[User]] = db.run(userTable.result)

  def createTableIfNotExisted {
    val x = exec(MTable.getTables("usertable")).toList
    if (x.isEmpty) {
      exec(createTableAction)
    }
  }


  def signUp(user: User) = {
    createTableIfNotExisted
    val hashedPassword = BCrypt.hashpw(user.password, BCrypt.gensalt())
    insertUserWithUserInfo(User(user.id, user.email, hashedPassword), "EMPTY", "EMPTY")
  }


  def findByEmailAddress(emailAddress: String): Option[User] = {
    exec(userTable.filter(_.email === emailAddress).result.headOption)
  }

  def findById(id: Long): Option[User] = {
    exec(userTable.filter(_.id === id).result.headOption)
  }

  def isUserExisted(emailAddress: String): Boolean = {
    createTableIfNotExisted
    val user = findByEmailAddress(emailAddress)
    user.isDefined
  }

  def checkUser(user: User): Boolean = {
    createTableIfNotExisted
    val tempOptionUser = findByEmailAddress(user.email)
    if (tempOptionUser.isDefined) {
      val knownUser = tempOptionUser.get
      return BCrypt.checkpw(user.password, knownUser.password)
    }
    false
  }

  private val USER_INFO_TABLE_NAME = "userinfotable"

  class UserInfoTable(tag: Tag) extends Table[UserInfo](tag, USER_INFO_TABLE_NAME) {

    def name = column[String]("name")

    def location = column[String]("location")

    def userId = column[Long]("userId")

    def * = (userId, name, location) <> (UserInfo.tupled, UserInfo.unapply)

    def album = foreignKey("album_fk", userId, userTable)(_.id, onDelete = ForeignKeyAction.Cascade)

  }

  private lazy val createUserInfoTableAction = userInfoTable.schema.create

  def createUserInfoTableIfNotExisted {
    val x = exec(MTable.getTables(USER_INFO_TABLE_NAME)).toList
    if (x.isEmpty) {
      exec(createUserInfoTableAction)
    }
  }


  private val insertUser = userTable returning userTable.map(_.id)
  private val userInfoTable = TableQuery[UserInfoTable]

  def insertUserWithUserInfo(user: User, name: String = "EMPTY", location: String = "EMPTY"): Boolean = {
    createUserInfoTableIfNotExisted

    if (isUserExisted(user.email)) return false
    val insertAction = for {
      userId <- insertUser += user
      count <- userInfoTable ++= Seq(
        UserInfo(userId, name, location)
      )
    } yield count

    exec(insertAction)
    true
  }


  def getUserInfo(user: User): UserInfo = {
    val get = for {
      userId <- userTable.filter(_.id === user.id).result.head
      rowsAffected <- userInfoTable.filter(_.userId === userId.id).result.head
    } yield rowsAffected
    exec(get)
  }

  def updateUserInfo(user: User, userInfo: UserInfo) = {
    val userInfoToUpdate: UserInfo = userInfo.copy(user.id.get)
    val update = userInfoTable.filter(_.userId === user.id.get).update(userInfoToUpdate)
    exec(update)
  }

  def deleteUser(email: String) = {
    val deleteAction = userTable.filter(_.email === email).delete
    exec(deleteAction)
  }

}
