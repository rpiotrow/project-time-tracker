package io.github.rpiotrow.ptt.api

package object error {

  sealed trait ApiError
  case class NotFound(what: String)     extends ApiError
  case class InvalidInput(what: String) extends ApiError
  case object Unauthorized              extends ApiError
  case class Forbidden(what: String)    extends ApiError
  case class Conflict(what: String)     extends ApiError
  case class ServerError(what: String)  extends ApiError

}
