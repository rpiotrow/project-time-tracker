package io.github.rpiotrow.ptt.api.write

import zio.{Has, ZLayer}

package object service {
  type ProjectService = Has[ProjectService.Service]

  type Services = ProjectService

  sealed trait AppFailure
  case class EntityNotFound(what: String) extends AppFailure
  case class NotUnique(what: String)      extends AppFailure
  case class AppThrowable(ex: Throwable)  extends AppFailure

  val liveServices: ZLayer[Any, Throwable, Services] =
    ZLayer.fromFunction(_ => ProjectService.live())
}
