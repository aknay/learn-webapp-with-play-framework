package dao

/**
  * Created by aknay on 2/3/17.
  */

import java.sql.Timestamp
import javax.inject.Inject

import com.google.inject.Singleton
import slick.jdbc.meta.MTable
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import models._
import org.joda.time.DateTime

import scala.concurrent.Await
import scala.concurrent.duration._

/** Ref: http://slick.lightbend.com/doc/3.0.0/schemas.html */

import slick.driver.PostgresDriver.api._

@Singleton
class AdminToolDao @Inject()(userDao: UserDao)(protected val dbConfigProvider: DatabaseConfigProvider) extends UserTableComponent with HasDatabaseConfigProvider[JdbcProfile] {
  /** describe the structure of the tables: */


  import driver.api._


  val ADMIN_TOOL_TABLE_NAME = "AdminToolTable"

  /** Since we are using album id as Option[Long], so we need to use id.? */
  class AdminToolTable(tag: Tag) extends Table[AdminTool](tag, ADMIN_TOOL_TABLE_NAME) {

    def staringDate = column[Option[DateTime]]("strtingDate", O.Default(None))

    def endingDate = column[Option[DateTime]]("endingDate", O.Default(None))

    def announcement = column[Option[String]]("announcement")

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def userId = column[Long]("userId")

    def * = (id.?, userId.?, staringDate, endingDate, announcement) <> (AdminTool.tupled, AdminTool.unapply)

    def fk = foreignKey("adimTool_fk", userId, userTable)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)

  lazy val adminToolTable = TableQuery[AdminToolTable]

  //Note: I cannot add this whole table as trait due to implicit
  //Either Inject dao error or it doesn't recognize the implicit value
  implicit val dateTimeTest = MappedColumnType.base[DateTime, String](
    { b => b.toString }, // map Date to String
    { i => DateTime.parse(i) } // map Sting to Date
  )

  this.createTableIfNotExisted()

  /** The following statements are Action */
  private lazy val createTableAction = adminToolTable.schema.create

  private val selectAlbumAction = adminToolTable.result

  def createTableIfNotExisted() {
    val x = exec(MTable.getTables(ADMIN_TOOL_TABLE_NAME)).toList
    if (x.isEmpty) {
      exec(createTableAction)
    }
  }

  def create(user: User): Boolean = {
    if (!userDao.isUserExisted(user.email)) return false
    if (isExist(user)) return false
    if (user.role != Role.Admin) return false
    exec(adminToolTable += AdminTool(Some(1), user.id, None, None, None))
    true
  }

  def isExist(user: User): Boolean = {
    val adminTool = exec(adminToolTable.filter(_.userId === user.id.get).map(_.id).result.headOption)
    adminTool.isDefined
  }

  def getAnnouncement(user: User): Option[String] = {
    val r = exec(adminToolTable.filter(_.userId === user.id.get).map(_.announcement).result.headOption)
    if (r.isDefined)  r.get
    else None
  }

  def getStartingDate(user: User): Option[DateTime] = {
    val test: Option[Option[DateTime]] = exec(adminToolTable.filter(_.userId === user.id.get).map(_.staringDate).result.headOption)
    if (test.isDefined)  test.get
    else None
  }

  def getEndingDate(user: User): Option[DateTime] = {
    val test: Option[Option[DateTime]] = exec(adminToolTable.filter(_.userId === user.id.get).map(_.endingDate).result.headOption)
    if (test.isDefined) test.get
    else None
  }

  def getAdminTool(user: User): Option[AdminTool] = {
    exec(adminToolTable.filter(_.userId === user.id.get).result.headOption)
  }

  def setStatingDateAndEndingDate(user: User, startingDate: DateTime, endingDate: DateTime): Boolean = {
    if (!isExist(user)) return false
    if (getAdminTool(user).isEmpty) return false
    val temp  = getAdminTool(user).get
    val tempCopy = temp.copy(startingDate=Some(startingDate), endingDate=Some(endingDate))
    val updateAction = adminToolTable.filter(_.userId === user.id).update(tempCopy)
    exec(updateAction)
    true
  }

  def setAnnouncement(user: User, announcement: String): Boolean = {
    if (!isExist(user)) return false
    if (getAdminTool(user).isEmpty) return false
    val temp  = getAdminTool(user).get
    val tempCopy = temp.copy(announcement=Some(announcement))
    val updateAction = adminToolTable.filter(_.userId === user.id).update(tempCopy)
    exec(updateAction)
    true
  }


}