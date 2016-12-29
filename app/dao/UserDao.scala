package dao

import javax.inject.Inject

import models.User
import org.mindrot.jbcrypt.BCrypt
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.tools.nsc.interpreter.session

/**
  * Created by aknay on 27/12/16.
  */
class UserDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  /** describe the structure of the tables: */
  /** Note: table cannot be named as 'user', otherwise we will problem with Postgresql */
  private val TABLE_NAME = "usertable"

  import driver.api._

  /** Since we are using album id as Option[Long], so we need to use id.? */
  class UserTable(tag: Tag) extends Table[User](tag, TABLE_NAME) {

    def email = column[String]("email")

    def password = column[String]("password")

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def * = (id.?, email, password) <> (User.tupled, User.unapply)

  }

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
    val hashedPassed = BCrypt.hashpw(user.password, BCrypt.gensalt())
    exec(userTable += User(user.id, user.email, hashedPassed))
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
    val tempOptionUser = findByEmailAddress(user.email)
    if (tempOptionUser.isDefined) {
      val knownUser = tempOptionUser.get
      return BCrypt.checkpw(user.password, knownUser.password)
    }
    false
  }
}
