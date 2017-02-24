package forms

import models.{User, Role}
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

}