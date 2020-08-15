package io.github.rpiotrow.ptt.write.web

import cats.implicits._
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

import scala.concurrent.ExecutionContext

trait Routes {
  def writeSideRoutes(): HttpRoutes[IO]
}

object Routes {

  def live(implicit contextShift: ContextShift[IO]): IO[Routes] = {
    val config = AppConfiguration.live
    for {
      projectService <- ProjectService.live
    } yield new RoutesLive(config.gatewayConfiguration.address, projectService)
  }

  def test(gatewayAddress: String, projectService: ProjectService)(implicit contextShift: ContextShift[IO]): Routes =
    new RoutesLive(gatewayAddress, projectService)
}

private class RoutesLive(private val gatewayAddress: String, private val projectService: ProjectService)(implicit
  private val contextShift: ContextShift[IO]
) extends Routes {

  implicit private val serverOptions: Http4sServerOptions[IO] =
    Http4sServerOptions.default[IO].copy(decodeFailureHandler = DecodeFailure.decodeFailureHandler)

  override def writeSideRoutes(): HttpRoutes[IO] = {
    val projectCreateRoute = projectCreateEndpoint
      .in(header[UserId]("X-Authorization"))
      .toRoutes { case (params, userId) => projectCreate(params, userId) }
    val projectDeleteRoute = projectDeleteEndpoint
      .in(header[UserId]("X-Authorization"))
      .toRoutes { case (projectId, userId) => projectDelete(projectId, userId) }

    projectCreateRoute <+> projectDeleteRoute
  }

  private def projectCreate(input: ProjectInput, userId: UserId): IO[Either[ApiError, LocationHeader]] = {
    projectService
      .create(input, userId)
      .bimap(
        {
          case NotUnique(m)      => InputNotValid(m)
          case NotOwner(m)       => Forbidden(m)
          case EntityNotFound(_) => NotFound
        },
        p => new LocationHeader(s"$gatewayAddress/projects/${p.projectId}")
      )
      .value
  }

  private def projectDelete(projectId: ProjectId, userId: UserId): IO[Either[ApiError, Unit]] = {
    projectService
      .delete(projectId, userId)
      .leftMap({
        case NotUnique(m)      => InputNotValid(m)
        case NotOwner(m)       => Forbidden(m)
        case EntityNotFound(_) => NotFound
      })
      .value
  }

}
