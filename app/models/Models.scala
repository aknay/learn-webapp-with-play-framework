package models

/**
  * Created by aknay on 14/12/16.
  */

case class Album(id: Option[Long] = None, artist: String, title: String)

case class User(id: Option[Long], email: String, password: String)

case class UserInfo(userId: Long, name: String, location: String)

