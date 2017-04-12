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

  //Ref: jodaDateFormat -> http://stackoverflow.com/questions/13953629/play-framework-2-1-scala-form-binding-for-date
  val announcementForm = Form(
    mapping(
      "id" -> ignored(None: Option[Long]),
      "userId" -> optional(longNumber),
      "startingDate" -> optional(jodaDate("dd-MM-yyyy")),
      "endingDate" -> optional(jodaDate("dd-MM-yyyy")),
      "announcement" -> optional(nonEmptyText),
      "lastUpdateTime" -> optional(jodaDate("dd-MM-yyyy")),
      "event" -> ignored(None: Option[String])
    )(AdminTool.apply)(AdminTool.unapply))


  val resetRequestViaEmailForm = Form(single("email" -> email))

  val resetPasswordForm = Form(tuple(
    "password1" -> nonEmptyText(minLength = 6),
    "password2" -> nonEmptyText
  ) verifying ("password is not equal", passwords => passwords._2 == passwords._1))

  val eventForm = Form(single("event" -> nonEmptyText))
}