package dao

/**
  * Created by s43132 on 2/12/2016.
  */

import javax.inject.Inject

import slick.jdbc.meta.MTable

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/** Ref: http://slick.lightbend.com/doc/3.0.0/schemas.html */

//import slick.driver.H2Driver.api._ //we cannot import both drivers at same place
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._

class AlbumDao @Inject()() {

  //create case class for Album; the format of how Album look like
  case class Album(artist: String,
                   title: String,
                   id: Long = 0L)


  //To define a mapped table that uses a custom type for
  // its * projection by adding a bi-directional mapping with the <> operator:

  //describe the structure of the tables:
  private val TABLE_NAME = "album"

  class AlbumTable(tag: Tag) extends Table[Album](tag, TABLE_NAME) {
    def artist = column[String]("artist")

    def title = column[String]("title")

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def * = (artist, title, id) <> (Album.tupled, Album.unapply)

  }

  //TableQuery value which represents the actual database table
  private lazy val AlbumTable = TableQuery[AlbumTable]

  /** The following statements are Action */
  private val createTableAction = AlbumTable.schema.create

  private val insertAlbumAction = AlbumTable ++= Seq(
    Album("Keyboard", "Pressed them"),
    Album("Mouse", "Click it")
  )

  private val getTableList = exec(MTable.getTables(TABLE_NAME)).toList

  private val selectAlbumAction = AlbumTable.result

  /** Ref: http://slick.lightbend.com/doc/3.0.0/database.html */

  //loading database configuration
  private val db = Database.forConfig("testpostgresql")

  //This is the blocking method with maximum waiting time of 2 seconds
  //This is also helper method for DBIO
  private def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)

  def printHello {println ("Hello")}

  def createTableIfNotExisted {
    try{
      if (getTableList.isEmpty) exec(createTableAction)
    }
    catch{
      case _ => println("we cannot create a table")
    }
  }

  def insertPredefinedData {exec(insertAlbumAction)}
  def printAllDataOnTable {exec(selectAlbumAction).foreach(println)}

}
