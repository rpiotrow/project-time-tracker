package io.github.rpiotrow.ptt.write

package object service {

  sealed trait AppFailure
  case class EntityNotFound(what: String) extends AppFailure
  case class NotUnique(what: String)      extends AppFailure
  case class AppThrowable(ex: Throwable)  extends AppFailure

}
