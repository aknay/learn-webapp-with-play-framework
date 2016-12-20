package models

/**
  * Created by aknay on 14/12/16.
  */
case class Album(artist: String,
                 title: String,
                 id: Long = 0L)

case class AlbumFormData(artist: String, title: String)