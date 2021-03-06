package models

/**
  * Created by aknay on 14/12/16.
  */

case class Album(id: Option[Long] = None, userId: Option[Long], artist: String, title: String)

case class User(id: Option[Long], email: String, password: String)

case class UserInfo(userId: Long, name: String, location: String)

case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
}