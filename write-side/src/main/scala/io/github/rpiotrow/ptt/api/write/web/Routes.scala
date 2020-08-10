package io.github.rpiotrow.ptt.api.write.web

import io.github.rpiotrow.ptt.api._
import io.github.rpiotrow.ptt.api.error._
import io.github.rpiotrow.ptt.api.input._
import io.github.rpiotrow.ptt.api.model._
import io.github.rpiotrow.ptt.api.output._
import io.github.rpiotrow.ptt.api.write.configuration.GatewayConfiguration
import io.github.rpiotrow.ptt.api.write.service._
import org.http4s.HttpRoutes
import sttp.tapir._
import sttp.tapir.server.http4s._
import zio._
import zio.config.ZConfig
import zio.interop.catz._

object Routes {
  trait Service {
    def readSideRoutes(): HttpRoutes[Task]
  }

  def readSideRoutes(): RIO[Routes, HttpRoutes[Task]] = ZIO.access(_.get.readSideRoutes())

  val live: ZLayer[Services with ZConfig[GatewayConfiguration], Throwable, Routes] =
    ZLayer.fromServices[GatewayConfiguration, ProjectService.Service, Routes.Service](
      (config, projectService) => new RoutesLive(config.address, projectService)
    )
}

private class RoutesLive(private val gatewayAddress: String, private val projectService: ProjectService.Service)
    extends Routes.Service {

  implicit private val serverOptions: Http4sServerOptions[Task] =
    Http4sServerOptions.default[Task].copy(decodeFailureHandler = DecodeFailure.decodeFailureHandler)

  override def readSideRoutes(): HttpRoutes[Task] = {
    projectCreateEndpoint
      .in(header[UserId]("X-Authorization"))
      .toRoutes { case (params, userId) => projectCreate(params, userId) }
  }

  private def projectCreate(input: ProjectInput, userId: UserId): Task[Either[ApiError, LocationHeader]] = {
    projectService
      .create(input, userId)
      .map(p => new LocationHeader(s"$gatewayAddress/projects/${p.id}"))
      .catchAll {
        case EntityNotFound(_) => ZIO.fail(NotFound)
        case NotUnique(m)      => ZIO.fail(InputNotValid(m))
        case AppThrowable(_)   => ZIO.fail(ServerError("server.error"))
      }
      .either
  }

}
