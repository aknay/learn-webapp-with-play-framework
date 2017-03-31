package dao

/**
  * Created by aknay on 2/3/17.
  */

import javax.inject.Inject
import com.google.inject.Singleton
import slick.jdbc.meta.MTable
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import models._
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

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

    def lastUpdateTime = column[Option[DateTime]]("lastUpdateTime")

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def adminId = column[Long]("adminId")

    def event = column[Option[String]]("event")

    def * = (id.?, adminId.?, staringDate, endingDate, announcement, lastUpdateTime, event) <> (AdminTool.tupled, AdminTool.unapply)

    //    def fk = foreignKey("adimTool_fk", adminId, userTable)(_.id, onDelete = ForeignKeyAction.Cascade)
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

  def createTableIfNotExisted() {
    val x = exec(MTable.getTables(ADMIN_TOOL_TABLE_NAME)).toList
    if (x.isEmpty) {
      exec(createTableAction)
    }
  }

  private def createAdminToolIfNotExisted: Boolean = {
    if (getAdminTool.isDefined) return false
    exec(adminToolTable += AdminTool(Some(1), Some(1), None, None, None, Some(DateTime.now()), None))
    true
  }

  def getAnnouncement: Option[String] = {
    if (getAdminTool.isEmpty) None
    getAdminTool.get.announcement
  }

  def getStartingDate: Option[DateTime] = {
    val test: Option[Option[DateTime]] = exec(adminToolTable.map(_.staringDate).result.headOption)
    if (test.isDefined) test.get else None
  }

  def getEndingDate: Option[DateTime] = {
    val test: Option[Option[DateTime]] = exec(adminToolTable.map(_.endingDate).result.headOption)
    if (test.isDefined) test.get else None
  }

  def getAdminTool: Option[AdminTool] = {
    exec(adminToolTable.result.headOption)
  }

  private def updateAdminTool(user: User, adminTool: AdminTool) = {
    val adminToolCopy = adminTool.copy(adminId = user.id, lastUpdateTime = Some(DateTime.now)) //always update with time and adminId
    exec(adminToolTable.update(adminToolCopy))
  }

  def deleteAllEvents(user: User) = {
    if (isValidToModifiedData(user)) {
      updateAdminTool(user, getAdminTool.get.copy(event = None))
    }
  }

  def makeAnnouncement(user: User, startingDate: DateTime, endingDate: DateTime, announcement: String): Boolean = {
    createAdminToolIfNotExisted
    if (!isValidToModifiedData(user)) return false
    val adminTool = getAdminTool.get
    val adminToolCopy = adminTool.copy(startingDate = Some(startingDate), endingDate = Some(endingDate),
      announcement = Some(announcement))
    updateAdminTool(user, adminToolCopy)
    true
  }

  private def isValidToModifiedData(user: User): Boolean = {
    if (getAdminTool.isEmpty) {
      println("there is no admin tool")
      return false
    }
    if (user.role != Role.Admin) {
      println("user is not admin")
      return false
    }
    true
  }

  val datePattern: DateTimeFormatter = DateTimeFormat.forPattern("dd-MMM-YYYY")

  def getFormattedDateString(date: DateTime): String = {
    date.toString(datePattern)
  }

  def getFormattedDate(date: String): DateTime = {
    DateTime.parse(date, datePattern)
  }

  def deleteAnnouncement(user: User): Boolean = {
    if (getAdminTool.isEmpty) return false
    val adminTool = getAdminTool.get.copy(startingDate = None, endingDate = None, announcement = None)
    updateAdminTool(user, adminTool)
    true
  }

  def isEventExisted(event: String, allEvents: String): Boolean = {
    val trimmedList = allEvents.split("\\s+")
    val result = trimmedList.filter(_.compareToIgnoreCase(event) == 0)
    if (result.length > 0) return true
    false
  }


  def getEvent: Option[String] = {
    if (getAdminTool.isEmpty) return None
    getAdminTool.get.event
  }


  def addEvent(user: User, event: String): Boolean = {
    createAdminToolIfNotExisted
    if (!isValidToModifiedData(user)) {
      print("we cant modified")
      return false
    }
    val adminTool = getAdminTool
    val allEvents = adminTool.get.event
    if (allEvents.isDefined) {
      if (isEventExisted(event, allEvents.get)) {
        print("event is already existed")
        return false
      }
      val modifiedAllEvents = allEvents.get + " " + event
      updateAdminTool(user, adminTool.get.copy(event = Some(modifiedAllEvents)))
    }
    else {
      updateAdminTool(user, adminTool.get.copy(event = Some(event)))
    }
    true
  }

}
