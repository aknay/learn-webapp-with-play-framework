package dao

/**
  * Created by aknay on 2/12/2016.
  */

import javax.inject.Inject

import com.google.inject.Singleton
import slick.jdbc.meta.MTable

import scala.concurrent._
import scala.concurrent.duration._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import models.{Album, Page, User}

import scala.concurrent.ExecutionContext.Implicits.global

/** Ref: http://slick.lightbend.com/doc/3.0.0/schemas.html */

//import slick.driver.H2Driver.api._ //we cannot import both drivers at same place
import slick.driver.PostgresDriver.api._

@Singleton
class AlbumDao @Inject()(userDao: UserDao)(protected val dbConfigProvider: DatabaseConfigProvider) extends UsersComponent with HasDatabaseConfigProvider[JdbcProfile] {

  //To define a mapped table that uses a custom type for
  // its * projection by adding a bi-directional mapping with the <> operator:
  //
  /** describe the structure of the tables: */
  private val TABLE_NAME = "album"
  this.createTableIfNotExisted
  userDao.createUserInfoTableIfNotExisted
  userDao.createUserTableIfNotExisted

  import driver.api._

  /** Since we are using album id as Option[Long], so we need to use id.? */
  class AlbumTable(tag: Tag) extends Table[Album](tag, TABLE_NAME) {
    def artist = column[String]("artist")

    def title = column[String]("title")

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def userId = column[Long]("userId")

    def * = (id.?, userId.?, artist, title) <> (Album.tupled, Album.unapply)

    def fk = foreignKey("album_fk", userId, userTable)(_.id, onDelete = ForeignKeyAction.Cascade)

  }

  //  //TableQuery value which represents the actual database table
  private lazy val albumTable = TableQuery[AlbumTable]
  private lazy val userTable = TableQuery[UserTable]

  /** The following statements are Action */
  private lazy val createTableAction = albumTable.schema.create

  private val selectAlbumAction = albumTable.result


  /** Ref: http://slick.lightbend.com/doc/3.0.0/database.html */
  //loading database configuration
  //  private val db = Database.forConfig("testpostgresql")

  //This is the blocking method with maximum waiting time of 2 seconds
  //This is also helper method for DBIO
  private def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)

  def getAlbumTable: Future[Seq[Album]] = db.run(albumTable.result)

  def createTableIfNotExisted() {
    val x = exec(MTable.getTables("album")).toList
    if (x.isEmpty) {
      exec(createTableAction)
    }
  }

  def printAllDataOnTable() {
    exec(selectAlbumAction).foreach(println)
  }

  def count(filter: String): Future[Int] = {
    db.run(albumTable.filter { album => album.artist like filter.toLowerCase }.length.result)
  }

  def listWithPage(userId: Long, page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%"): Future[Page[(Album)]] = {
    /** Ref:: http://slick.lightbend.com/doc/3.0.0/queries.html */
    val offset = pageSize * page
    val albumSize = retrieveByUserId(userId).size
    for {
      result <- retrieveByUserIdWitPageSize(userId, offset, pageSize)
    } yield Page(result, page, offset, albumSize)

  }

  def retrieveByUserIdWitPageSize(userId: Long, offset: Int, pagesize: Int): Future[Seq[Album]] = {
    db.run(albumTable.filter(_.userId === userId).drop(offset).take(pagesize).result)
  }

  def insertAlbum(album: Album, userId: Long): Boolean = {
    if (isAlbumExisted(album, userId)) return false
    val anotherAlbum: Album = Album(album.id, Some(userId), album.artist, album.title)
    exec(albumTable += anotherAlbum)
    true
  }

  def isAlbumExisted(album: Album, userId: Long): Boolean = {
    val albumTemp = exec(albumTable.filter(_.title === album.title).filter(_.artist === album.artist).filter(_.userId === userId).map(_.id).result.headOption)
    albumTemp.isDefined
  }

  def delete(album: Album, userId: Long): Int = {
    val deleteAction = for {
      id <- albumTable.filter(_.title === album.title).filter(_.artist === album.artist).filter(_.userId === userId).map(_.id).result.headOption
      beDeleted <- albumTable.filter(_.id === id).delete
    } yield beDeleted

    exec(deleteAction)
  }

  def retrieveByUserId(userId: Long): Seq[(String, String)] = {
    exec(albumTable.filter(_.userId === userId).map(x => (x.artist, x.title)).result)
  }

  def retrieveAlbumByUserId(userId: Long): Future[Seq[Album]] = {
    db.run(albumTable.filter(_.userId === userId).result)
  }

  def retrieveAlbumId(artist: String, title: String, userId: Long): Option[Long] = {
    exec(albumTable.filter(_.artist === artist).filter(_.title === title).filter(_.userId === userId).map(_.id).result.headOption)
  }

  def delete(id: Long) {
    val deleteAction = albumTable.filter(_.id === id).delete
    exec(deleteAction)
  }

  /** result.head is to get single result */
  def find(id: Long): Album = {
    exec(albumTable.filter(_.id === id).result.head)
  }

  /** we make album id as Option[Long]; so we have to use Some to get id */
  def update(id: Long, album: Album, userId: Long): Unit = {
    val albumToUpdate: Album = album.copy(Some(id))
    val anotherAlbum: Album = Album(albumToUpdate.id, Some(userId), albumToUpdate.artist, albumToUpdate.title)
    val updateAction = albumTable.filter(_.id === id).update(anotherAlbum)
    exec(updateAction)
  }

}
