package utils.Silhouette

/**
  * Created by s43132 on 21/2/2017.
  */

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.mvc.Request

import scala.concurrent.Future
import models.{Role, User}

/**
  * Only allows those users that have at least a service of the selected.
  * Master service is always allowed.
  * Ex: WithService("serviceA", "serviceB") => only users with services "serviceA" OR "serviceB" (or "master") are allowed.
  */
case class WithService(anyOf: String*) extends Authorization[User, CookieAuthenticator] {
  def isAuthorized[A](user: User, authenticator: CookieAuthenticator)(implicit r: Request[A]) = Future.successful {
    WithService.isAuthorized(user, anyOf: _*)
  }
}
object WithService {
  def isAuthorized(user: User, anyOf: String*): Boolean =
    anyOf.intersect(user.role).size > 0 || user.role.contains(Role.admin)
}

/**
  * Only allows those users that have every of the selected services.
  * Master service is always allowed.
  * Ex: Restrict("serviceA", "serviceB") => only users with services "serviceA" AND "serviceB" (or "master") are allowed.
  */
case class WithServices(allOf: String*) extends Authorization[User, CookieAuthenticator] {
  def isAuthorized[A](user: User, authenticator: CookieAuthenticator)(implicit r: Request[A]) = Future.successful {
    WithServices.isAuthorized(user, allOf: _*)
  }
}
object WithServices {
  def isAuthorized(user: User, allOf: String*): Boolean =
    allOf.intersect(user.role).size == allOf.size || user.role.contains(Role.admin)
}