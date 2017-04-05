package dao

import models.{Album, Role, User}

import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

/**
  * Created by aknay on 2/3/17.
  */

trait UserTableComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  val USER_TABLE_NAME = "usertable"

  import profile.api._

  /** Since we are using album id as Option[Long], so we need to use id.? */
  class UserTable(tag: Tag) extends Table[User](tag, USER_TABLE_NAME) {

    def email = column[String]("email")

    def password = column[String]("password")

    def username = column[String]("username")

    def activated = column[Boolean]("activated")

    def role = column[Role]("role")

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def * = (id.?, email, password, username, role, activated) <> (User.tupled, User.unapply)

  }

  lazy val userTable = TableQuery[UserTable]

}

trait AlbumTableComponent extends UserTableComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  val ALBUM_TABLE_NAME = "album"

  /** Since we are using album id as Option[Long], so we need to use id.? */
  class AlbumTable(tag: Tag) extends Table[Album](tag, ALBUM_TABLE_NAME) {
    def artist = column[String]("artist")

    def title = column[String]("title")

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def userId = column[Long]("userId")

    def * = (id.?, userId.?, artist, title) <> (Album.tupled, Album.unapply)

    def fk = foreignKey("album_fk", userId, userTable)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  lazy val albumTable = TableQuery[AlbumTable]
}



