package io.github.rpiotrow.ptt.api

package object error {

  sealed trait ApiError
  case object NotFound                      extends ApiError
  case object Unauthorized                  extends ApiError
  case class Forbidden(what: String)        extends ApiError
  case class InputNotValid(details: String) extends ApiError
  case class ServerError(what: String)      extends ApiError

}
