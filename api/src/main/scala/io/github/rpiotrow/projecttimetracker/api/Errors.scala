package io.github.rpiotrow.projecttimetracker.api

object Errors {

  sealed trait ApiError
  case object NotFound extends ApiError
  case object Unauthorized extends ApiError
  case class InputNotValid(details: String) extends ApiError
  case class ServerError(what: String) extends ApiError

}
