package forms

import models.{AdminTool, Album, Role, User}
import play.api.data.Form
import play.api.data.Forms._

/**
  * Created by aknay on 30/1/17.
  */
object Forms {

  val signUpForm = Form(
    mapping(
      "id" -> ignored(None: Option[Long]),
      "email" -> email,
      "password" -> nonEmptyText,
      "username" -> nonEmptyText,
      "role" -> ignored(Role.NormalUser: Role),
      "activated" -> ignored(false)
    )(User.apply)(User.unapply))

  val loginForm = Form(
    mapping(
      "id" -> ignored(None: Option[Long]),
      "email" -> email,
      "password" -> nonEmptyText,
      "username" -> ignored(""),
      "role" -> ignored(Role.NormalUser: Role),
      "activated" -> ignored(false)
    )(User.apply)(User.unapply))

  val albumForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "userId" -> optional(longNumber),
      "artist" -> nonEmptyText,
      "title" -> nonEmptyText

    )(Album.apply)(Album.unapply)
  )

  val announcementForm = Form(
    mapping(
      "id" -> ignored(None: Option[Long]),
      "userId" -> optional(longNumber),
      "startingDate" -> optional(jodaDate),
      "endingDate" -> optional(jodaDate),
      "announcement" -> optional(nonEmptyText)
    )(AdminTool.apply)(AdminTool.unapply))

}