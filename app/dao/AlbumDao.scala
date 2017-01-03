package dao

/**
  * Created by aknay on 2/12/2016.
  */

import javax.inject.Inject
import slick.jdbc.meta.MTable
import scala.concurrent._
import scala.concurrent.duration._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import models.Album

/** Ref: http://slick.lightbend.com/doc/3.0.0/schemas.html */

//import slick.driver.H2Driver.api._ //we cannot import both drivers at same place
import slick.driver.PostgresDriver.api._

class AlbumDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  //To define a mapped table that uses a custom type for
  // its * projection by adding a bi-directional mapping with the <> operator:
  //
  /** describe the structure of the tables: */
  private val TABLE_NAME = "album"

  import driver.api._

  /** Since we are using album id as Option[Long], so we need to use id.? */
  class AlbumTable(tag: Tag) extends Table[Album](tag, TABLE_NAME) {
    def artist = column[String]("artist")

    def title = column[String]("title")

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def * = (id.?, artist, title) <> (Album.tupled, Album.unapply)

  }

  //  //TableQuery value which represents the actual database table
  private lazy val AlbumTable = TableQuery[AlbumTable]

  /** The following statements are Action */
  private lazy val createTableAction = AlbumTable.schema.create

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
    }
  }

  def printAllDataOnTable {
    exec(selectAlbumAction).foreach(println)
  }

  def insertAlbum(album: Album) = {
    exec(AlbumTable += album)
  }

  def delete(id: Long) {
    val deleteAction = AlbumTable.filter(_.id === id).delete
    exec(deleteAction)
  }

  /** result.head is to get single result */
  def find(id: Long): Album = {
    exec(AlbumTable.filter(_.id === id).result.head)
  }

  /** we make album id as Option[Long]; so we have to use Some to get id */
  def update(id: Long, album: Album): Unit = {
    val albumToUpdate: Album = album.copy(Some(id))
    val updateAction = AlbumTable.filter(_.id === id).update(albumToUpdate)
    exec(updateAction)
  }

}
