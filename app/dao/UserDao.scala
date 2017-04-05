package dao

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent._
import scala.concurrent.duration._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.meta.MTable
import org.mindrot.jbcrypt.BCrypt
import models.{User, UserInfo, Role}

/**
  * Created by aknay on 27/12/16.
  */
/** change to traits so that other dao can access this user dao */
/** Ref:https://github.com/playframework/play-slick/blob/master/samples/computer-database/app/dao/CompaniesDAO.scala */


@Singleton()
class UserDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends UserTableComponent with HasDatabaseConfigProvider[JdbcProfile] {

  /** describe the structure of the tables: */
  /** Note: table cannot be named as 'user', otherwise we will problem with Postgresql */
  this.createUserTableIfNotExisted
  this.createUserInfoTableIfNotExisted

  import profile.api._

  /** The following statements are Action */
  private lazy val createTableAction = userTable.schema.create

  private val selectAlbumAction = userTable.result

  /** Ref: http://slick.lightbend.com/doc/3.0.0/database.html */

  //This is the blocking method with maximum waiting time of 2 seconds
  //This is also helper method for DBIO
  private def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)

  private def blockExec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)

  def getUserTable: Future[Seq[User]] = db.run(userTable.result)

  createUserInfoTableIfNotExisted

  def createUserTableIfNotExisted {
    val x = exec(MTable.getTables(USER_TABLE_NAME)).toList
    if (x.isEmpty) {
      exec(createTableAction)
    }
  }

  def getNonAdminUserList(): Seq[User] = {
    exec(userTable.filter(_.role =!= (Role.Admin: Role)).result)
  }

  def insertUserWithHashPassword(user: User): Future[Boolean] = {
    val hashedPassword = BCrypt.hashpw(user.password, BCrypt.gensalt())
    val userCopy = user.copy(password = hashedPassword)
    addUser(userCopy)
  }

  def getUserByEmail(email: String): Future[Option[User]] = {
    db.run(userTable.filter(_.email === email).result.headOption)
  }

  def findById(id: Long): Option[User] = {
    exec(userTable.filter(_.id === id).result.headOption)
  }

  def isUserExisted(email: String): Future[Boolean] = {
    for {
      user <- getUserByEmail(email)
      result <- Future(user.isDefined)
    } yield result
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

  def addUser(user: User): Future[Boolean] = {
    val isExisted: Future[Boolean] = for {
      a <- isUserExisted(user.email)
    } yield a

    isExisted.map {
      case true => {
        false
      }
      case false => {
        val insertAction = for {
          userId <- insertUser += user
          count <- userInfoTable ++= Seq(
            UserInfo(userId, "EMPTY", "EMPTY")
          )
        } yield count
        db.run(insertAction)
        true
      }
    }
  }

  def insertUserInfo(user: User, name: String = "", location: String = ""): Unit = {
    createUserInfoTableIfNotExisted
    val insertAction = userInfoTable ++= Seq(UserInfo(user.id.get, name, location))
    exec(insertAction)
  }

  def saveUserByLoginInfo(user: User): Future[User] = {
    val isUserExist: Future[Boolean] = for {
      a <- isUserExisted(user.email)
    } yield a

    isUserExist.flatMap {
      case true => updateUserByLoginInfo(user)
      case false => db.run(insertUser += user).map {
        _ => user
      }
    }
  }

  def getUserInfo(user: User): Future[Option[UserInfo]] = {
    val get = for {
      userId <- userTable.filter(_.id === user.id).result.head
      rowsAffected <- userInfoTable.filter(_.userId === userId.id).result.headOption
    } yield rowsAffected
    db.run(get)
  }

  def updateUserInfo(user: User, name: String, location: String): Future[Int] = {
    val userInfo = for {
      userInfo <- getUserInfo(user)
    } yield userInfo

    userInfo.flatMap{
      case Some(a) => db.run(userInfoTable.filter(_.userId === user.id.get).update(a.copy(name = name, location = location)))
    }
  }

  def removeUser(email: String): Future[Int] = {
    db.run(userTable.filter(_.email === email).delete)
  }

  def deleteUserByEmail(email: String): Future[Unit] = {
    db.run(userTable.filter(_.email === email).delete).map { _ => () }
  }

  def updateUserByLoginInfo(user: User): Future[User] = {
    val userToUpdate = user.copy(password = user.password)
    val update = userTable.filter(_.email === user.email).update(userToUpdate)

    db.run(update).map {
      _ => user
    }
  }

  /////////////////////////////////////////BLOCKING METHOD////////////////////////////////////
  def getUserByEmailWithBlocking(email: String) = {
    blockExec(userTable.filter(_.email === email).result.headOption)
  }

  def isUserExistedWithBlocking(emailAddress: String): Boolean = {
    val user = getUserByEmailWithBlocking(emailAddress)
    user.isDefined
  }

  def insertUserWithUserInfoWithBlocking(user: User, name: String = "EMPTY", location: String = "EMPTY"): Boolean = {
    if (isUserExistedWithBlocking(user.email)) return false
    val insertAction = for {
      userId <- insertUser += user
      count <- userInfoTable ++= Seq(UserInfo(userId, name, location)
      )
    } yield count

    blockExec(insertAction)
    true
  }

  def addUserWithBlocking(user: User): Boolean = {
    if (isUserExistedWithBlocking(user.email)) return false
    val insertAction = for {
      userId <- insertUser += user
      count <- userInfoTable ++= Seq(
        UserInfo(userId, "EMPTY", "EMPTY")
      )
    } yield count
    blockExec(insertAction)
    true
  }


}
