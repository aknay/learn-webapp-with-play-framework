package dao

/**
  * Created by aknay on 2/3/17.
  */

import java.io
import javax.inject.Inject

import com.google.inject.Singleton
import slick.jdbc.meta.MTable
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import models._
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._

/** Ref: http://slick.lightbend.com/doc/3.0.0/schemas.html */

@Singleton
class AdminToolDao @Inject()(userDao: UserDao)(protected val dbConfigProvider: DatabaseConfigProvider) extends UserTableComponent with HasDatabaseConfigProvider[JdbcProfile] {
  /** describe the structure of the tables: */


  import profile.api._


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
  }

    def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)


  lazy val adminToolTable = TableQuery[AdminToolTable]

  //Note: I cannot add this whole table as trait due to implicit
  //Either Inject dao error or it doesn't recognize the implicit value
  implicit val dateTimeTest = MappedColumnType.base[DateTime, String](
    { b => b.toString }, // map Date to String
    { i => DateTime.parse(i) } // map Sting to Date
  )

    this.createTableIfNotExisted
  this.createAdminToolIfNotExisted

  /** The following statements are Action */
  private lazy val createTableAction = adminToolTable.schema.create

    def createTableIfNotExisted {
      val x = exec(MTable.getTables(ADMIN_TOOL_TABLE_NAME)).toList
      if (x.isEmpty) {
        exec(createTableAction)
      }
    }

  def createAdminToolIfNotExisted = {
    getAdminTool.map { a => {
      if (a.isEmpty) db.run(adminToolTable += AdminTool(Some(1), Some(1), None, None, None, Some(DateTime.now()), None))
      else println("status" + a.isDefined)
    }
    }
  }

  def getAnnouncement: Future[Option[Option[String]]] = {
    db.run(adminToolTable.map(_.announcement).result.headOption)
  }

  def getStartingDate: Future[Option[Option[DateTime]]] = {
    db.run(adminToolTable.map(_.staringDate).result.headOption)
  }

  def getEndingDate: Future[Option[Option[DateTime]]] = {
    db.run(adminToolTable.map(_.endingDate).result.headOption)
  }

  def getAdminTool: Future[Option[AdminTool]] = {
    db.run(adminToolTable.result.headOption)
  }

  def deleteAdminTool {
    db.run(adminToolTable.delete)
  }

  def updateAdminTool(user: User, adminTool: AdminTool): Future[Int] = {
    val adminToolCopy = adminTool.copy(adminId = user.id, lastUpdateTime = Some(DateTime.now)) //always update with time and adminId
    println("copy in update" + adminToolCopy.announcement.get)
    db.run(adminToolTable.update(adminToolCopy))
  }

  def deleteAllEvents(user: User) = {
    if (isValidToModifiedData(user)) {

      val t = for {
        copy <- getAdminTool
        n <- updateAdminTool(user, copy.get.copy(event = None))
      } yield {
      }
    }
  }

  private def isValidToModifiedData(user: User): Boolean = {
    if (user.role != Role.Admin) {
      println("user is not admin")
      return false
    }
    true
  }

  val DATE_PATTERN: DateTimeFormatter = DateTimeFormat.forPattern("dd-MMM-YYYY")

  def getFormattedDateString(date: DateTime): String = {
    date.toString(DATE_PATTERN)
  }

  def getFormattedDate(date: String): DateTime = {
    DateTime.parse(date, DATE_PATTERN)
  }

  def createAnnouncement(user: User, announcement: String, startingDate: DateTime, endingDate: DateTime): Future[Unit] = {
    for {
      adminTool <- getAdminTool
      result <- updateAdminTool(user, adminTool.get.copy(announcement = Some(announcement), startingDate = Some(startingDate), endingDate = Some(endingDate)))
    } yield {}
  }

  def deleteAnnouncement(user: User): Future[Unit] = {
    for {
      adminTool <- getAdminTool
      result <- db.run(adminToolTable.update(adminTool.get.copy(announcement = None, startingDate = None, endingDate = None)))
    } yield {}
  }



  def isEventExisted(event: String, allEvents: String): Boolean = {
    val trimmedList = allEvents.split("\\s+")
    val result = trimmedList.filter(_.compareToIgnoreCase(event) == 0)
    if (result.length > 0) return true
    false
  }


  def getEvent: Future[Option[Option[String]]] = {
    db.run(adminToolTable.map(_.event).result.headOption)
  }


  //  def getEventsAsList: Future[Future[Product with io.Serializable]] = {
  //    getEvent.map{
  //      events =>
  //      events match {
  //        case None => Future(Some(None))
  //        case Some(c) => Future(events.get.get.split("\\s+").toList)
  //      }
  //    }
  //  }


  //  def getEventsAsList: Option[List[String]] = {
  //
  //    val result: Future[Nothing] = for {
  //      str: Option[Option[String]] <- getEvent
  //      re <- str.get.get.split("\\s+").toList
  //    } yield re
  //
  //
  ////    if (getAdminTool.isEmpty) return None
  ////    if (getAdminTool.get.event.isEmpty) return None
  ////    Some(getAdminTool.get.event.get.split("\\s+").toList)
  //  }

  def addEvent(user: User, event: String) = {
    //    createAdminToolIfNotExisted
    //    if (!isValidToModifiedData(user)) {
    //      print("we cant modified")
    //      return false
    //    }


    //    val r = for {
    //      adminTool <- getAdminTool
    //      allEvents <- adminTool.get.event if adminTool.isDefined
    //      result <- isEventExisted(event, allEvents) if adminTool.isDefined && adminTool.get.event.isDefined
    //    } yield result


    val r = for {
      adminTool <- getAdminTool
      //      allEvents <- adminTool.get.event
      result <- Future(isEventExisted(event, adminTool.get.event.get))

    //      t <- updateAdminTool(user,adminTool.get) if result
    } yield result


    val result = r.map {
      t => t

    }


    //    r.map{
    //      t => {
    //        if (t)  updateAdminTool(user, getAdminTool.get.copy(event = Some(modifiedAllEvents)))
    //      }
    //    }

  }


  //    val adminTool = getAdminTool
  //    val allEvents = adminTool.get.event
  //    if (allEvents.isDefined) {
  //      if (isEventExisted(event, allEvents.get)) {
  //        print("event is already existed")
  //        return false
  //      }
  //      val modifiedAllEvents = allEvents.get + " " + event
  //      updateAdminTool(user, adminTool.get.copy(event = Some(modifiedAllEvents)))
  //    }
  //    else {
  //      updateAdminTool(user, adminTool.get.copy(event = Some(event)))
  //    }
  //    true
  //  }

}
