package io.github.rpiotrow.ptt.write.web

import cats.effect.{ContextShift, IO}
import io.github.rpiotrow.ptt.api._
import io.github.rpiotrow.ptt.api.error._
import io.github.rpiotrow.ptt.api.input._
import io.github.rpiotrow.ptt.api.model._
import io.github.rpiotrow.ptt.api.output._
import io.github.rpiotrow.ptt.write.configuration.AppConfiguration
import io.github.rpiotrow.ptt.write.service._
import org.http4s.HttpRoutes
import sttp.tapir._
import sttp.tapir.server.http4s._

trait Routes {
  def writeSideRoutes(): HttpRoutes[IO]
}

object Routes {

  def live(implicit contextShift: ContextShift[IO]): IO[Routes] =
    for {
      config         <- AppConfiguration.live
      projectService <- ProjectService.live
    } yield new RoutesLive(config.gatewayConfiguration.address, projectService)

  def test(gatewayAddress: String, projectService: ProjectService)(implicit contextShift: ContextShift[IO]): Routes =
    new RoutesLive(gatewayAddress, projectService)
}

private class RoutesLive(private val gatewayAddress: String, private val projectService: ProjectService)(implicit
  private val contextShift: ContextShift[IO]
) extends Routes {

  implicit private val serverOptions: Http4sServerOptions[IO] =
    Http4sServerOptions.default[IO].copy(decodeFailureHandler = DecodeFailure.decodeFailureHandler)

  override def writeSideRoutes(): HttpRoutes[IO] = {
    projectCreateEndpoint
      .in(header[UserId]("X-Authorization"))
      .toRoutes { case (params, userId) => projectCreate(params, userId) }
  }

  private def projectCreate(input: ProjectInput, userId: UserId): IO[Either[ApiError, LocationHeader]] = {
    projectService
      .create(input, userId)
      .bimap(
        {
          case EntityNotFound(_) => NotFound
          case NotUnique(m)      => InputNotValid(m)
          case AppThrowable(_)   => ServerError("server.error")
        },
        p => new LocationHeader(s"$gatewayAddress/projects/${p.id}")
      )
      .value
  }

}
