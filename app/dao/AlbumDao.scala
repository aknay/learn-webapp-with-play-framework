package dao

/**
  * Created by aknay on 2/12/2016.
  */

import javax.inject.Inject

import slick.jdbc.meta.MTable

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.Future
import models.Album
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

/** Ref: http://slick.lightbend.com/doc/3.0.0/schemas.html */

//import slick.driver.H2Driver.api._ //we cannot import both drivers at same place
import slick.driver.PostgresDriver.api._
class AlbumDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
//class AlbumDao  {

  //To define a mapped table that uses a custom type for
  // its * projection by adding a bi-directional mapping with the <> operator:
  //
  //describe the structure of the tables:
  private val TABLE_NAME = "album"
  import driver.api._

  class AlbumTable(tag: Tag) extends Table[Album](tag, TABLE_NAME) {
    def artist = column[String]("artist")

    def title = column[String]("title")

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def * = (artist, title, id) <> (Album.tupled, Album.unapply)

  }

  //  //TableQuery value which represents the actual database table
  private lazy val AlbumTable = TableQuery[AlbumTable]

  /** The following statements are Action */
  private lazy val createTableAction = AlbumTable.schema.create

  private val insertAlbumAction = AlbumTable ++= Seq(
    Album("Keyboard", "Pressed them"),
    Album("Mouse", "Click it")
  )

  private val selectAlbumAction = AlbumTable.result

   /** Ref: http://slick.lightbend.com/doc/3.0.0/database.html */
  //loading database configuration
//  private val db = Database.forConfig("testpostgresql")

  //This is the blocking method with maximum waiting time of 2 seconds
  //This is also helper method for DBIO
  private def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)

  def getAlbumTable: Future[Seq[Album]] = db.run(AlbumTable.result)

  def createTableIfNotExisted {
    val x = exec(MTable.getTables("album")).toList
    if (x.isEmpty) {
      exec(createTableAction)
      exec(insertAlbumAction)
    }

  }
  def insertPredefinedData {exec(insertAlbumAction)}
  def printAllDataOnTable {exec(selectAlbumAction).foreach(println)}

}
