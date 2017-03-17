package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import play.api.data.Form
import play.api.mvc.Flash
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import dao.{AdminToolDao, AlbumDao, UserDao}
import forms.Forms
import models.{Album, User}
import org.joda.time.DateTime
import utils.Silhouette.{AuthController, MyEnv}

/**
  * Created by aknay on 5/12/2016.
  */
/** we need to use I18nSupport because we are using form helper
  * Without this --> Form: could not find implicit value for parameter messages: play.api.i18n.Messages
  * Ref: http://stackoverflow.com/questions/30799988/play-2-4-form-could-not-find-implicit-value-for-parameter-messages-play-api-i
  * */
class AlbumController @Inject()(albumDao: AlbumDao, userDao: UserDao, adminToolDao: AdminToolDao)
                               (val messagesApi: MessagesApi, val silhouette: Silhouette[MyEnv])
  extends AuthController with I18nSupport {
  /** we can use album form directly with Album case class by applying id as Option[Long] */

  def isItAllowedToModify: Boolean = {
    val adminTool = adminToolDao.getLatestUpdatedAdminTool()
    if (adminTool.isEmpty) return true
    val currentTime = DateTime.now()
    val startingTime = adminTool.get.startingDate.get
    val endingTime = adminTool.get.endingDate.get
    val resultStartingTime = currentTime.compareTo(startingTime)
    val resultEndingTime = currentTime.compareTo(endingTime)
    resultEndingTime < 0 && resultStartingTime > 0
  }

  def add = SecuredAction { implicit request =>
    val user = Some(request.identity)
    if (isItAllowedToModify) Ok(views.html.AlbumView.add(user)) else Ok(views.html.AlbumView.notallowed(user))
  }

  def delete(id: Long) = SecuredAction { implicit request =>
    if (isItAllowedToModify) {
      albumDao.delete(id)
      Redirect(routes.UserController.user())
    }
    else Ok(views.html.AlbumView.notallowed(Some(request.identity)))
  }

  def update(id: Long) = SecuredAction { implicit request =>
    val newAlbumForm = Forms.albumForm.bindFromRequest()
    newAlbumForm.fold(
      hasErrors = { form =>
        println("we are having error, try to check form data is matched with html")
        println(form.data)
        Redirect(routes.HomeController.index())
      },
      success = { _ =>
        val loginUser: User = userDao.getUserByEmailAddress(request.identity.email).get
        albumDao.update(id, newAlbumForm.get, loginUser.id.get)
        Redirect(routes.UserController.user())
      })
  }

  def edit(id: Long) = SecuredAction { implicit request =>
    val user = Some(request.identity)
    if (isItAllowedToModify) {
      val album: Album = albumDao.find(id)
      val form: Form[Album] = Forms.albumForm.fill(album)
      Ok(views.html.AlbumView.edit(user, id, form))
    }
    else Ok(views.html.AlbumView.notallowed(user))
  }

  def save = SecuredAction { implicit request =>

    val newProductForm = Forms.albumForm.bindFromRequest()

    newProductForm.fold(
      hasErrors = { form =>
        println("we are having error, try to check form data is matched with html")
        println(form.data)
        Redirect(routes.HomeController.index())
      },
      success = {
        newAlbum =>
          val loginUser: User = userDao.getUserByEmailAddress(request.identity.email).get
          val isSavingAlbumSuccessful = albumDao.insertAlbum(newAlbum, loginUser.id.get)
          if (isSavingAlbumSuccessful) Redirect(routes.UserController.user())
          else Redirect(routes.AlbumController.add()) flashing (Flash(newProductForm.data) +
            ("error" -> Messages("album.alreadyExisted.error")))
      })
  }

}