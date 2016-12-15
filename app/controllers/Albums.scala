package controllers

import play.api.data.Form
import play.api.data._
import play.api.data.Forms._

/**
  * Created by aknay on 5/12/2016.
  */
class Albums {

  case class Album(artist: String, title: String)

  val artistForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "album" -> nonEmptyText
    )(Album.apply)(Album.unapply)
  )

}
