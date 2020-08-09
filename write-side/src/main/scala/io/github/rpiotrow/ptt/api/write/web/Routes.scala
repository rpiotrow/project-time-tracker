package io.github.rpiotrow.ptt.api.write.web

import io.github.rpiotrow.ptt.api._
import io.github.rpiotrow.ptt.api.error._
import io.github.rpiotrow.ptt.api.input.ProjectInput
import io.github.rpiotrow.ptt.api.output._
import io.github.rpiotrow.ptt.api.write.configuration.{AppConfiguration, GatewayConfiguration}
import io.github.rpiotrow.ptt.api.write.service._
import org.http4s.HttpRoutes
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
    projectCreateEndpoint.toRoutes { params =>
      projectCreate(params)
    }
  }

  private def projectCreate(params: ProjectInput): Task[Either[ApiError, LocationHeader]] = {
    projectService
      .create(params)
      .map(p => new java.net.URL(s"$gatewayAddress/projects/${p.id}"))
      .either
  }

}
