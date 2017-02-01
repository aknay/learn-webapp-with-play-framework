package forms

import models.User
import play.api.data.Form
import play.api.data.Forms._

/**
  * Created by aknay on 30/1/17.
  */
object SignUpForm {

  val form = Form(
    mapping(
      "id" -> ignored(None: Option[Long]),
      "email" -> email,
      "password" -> nonEmptyText,
      "activated" -> ignored(false)
    )(User.apply)(User.unapply))
}
