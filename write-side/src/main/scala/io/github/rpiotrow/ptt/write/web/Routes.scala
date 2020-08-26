package io.github.rpiotrow.ptt.write.web

import cats.implicits._
import cats.effect.{Async, ContextShift, Resource}
import io.github.rpiotrow.ptt.api._
import io.github.rpiotrow.ptt.api.error._
import io.github.rpiotrow.ptt.api.input._
import io.github.rpiotrow.ptt.api.model._
import io.github.rpiotrow.ptt.api.output._
import io.github.rpiotrow.ptt.write.configuration.AppConfiguration
import io.github.rpiotrow.ptt.write.service._
import org.http4s.{EntityBody, HttpRoutes}
import sttp.tapir._
import sttp.tapir.server.http4s._

trait Routes[F[_]] {
  def writeSideRoutes(): HttpRoutes[F]
}

object Routes {

  def live[F[_]: Async: ContextShift](): Resource[F, Routes[F]] = {
    val gatewayConfiguration = AppConfiguration.live.gatewayConfiguration
    services[F]().map({
      case (projectService, taskService) =>
        new RoutesLive[F](gatewayConfiguration.address, projectService, taskService)
    })
  }

  def test[F[_]: Async: ContextShift](
    gatewayAddress: String,
    projectService: ProjectService[F],
    taskService: TaskService[F]
  ): Routes[F] =
    new RoutesLive[F](gatewayAddress, projectService, taskService)
}

private class RoutesLive[F[_]: Async](
  private val gatewayAddress: String,
  private val projectService: ProjectService[F],
  private val taskService: TaskService[F]
)(implicit private val contextShift: ContextShift[F])
    extends Routes[F] {

  implicit private val serverOptions: Http4sServerOptions[F] =
    Http4sServerOptions.default[F].copy(decodeFailureHandler = DecodeFailure.decodeFailureHandler)

  override def writeSideRoutes(): HttpRoutes[F] = {
    val projectCreateRoute = projectCreateEndpoint
      .withUserId()
      .toRoutes { case (params, userId) => projectCreate(params, userId) }
    val projectUpdateRoute = projectUpdateEndpoint
      .withUserId()
      .toRoutes { case ((projectId, input), userId) => projectUpdate(projectId, input, userId) }
    val projectDeleteRoute = projectDeleteEndpoint
      .withUserId()
      .toRoutes { case (projectId, userId) => projectDelete(projectId, userId) }
    val taskCreateRoute    = taskCreateEndpoint
      .withUserId()
      .toRoutes { case ((projectId, taskInput), userId) => taskAdd(projectId, taskInput, userId) }
    val taskUpdateRoute    = taskUpdateEndpoint
      .withUserId()
      .toRoutes { case ((projectId, taskId, taskInput), userId) => taskUpdate(projectId, taskId, taskInput, userId) }
    val taskDeleteRoute    = taskDeleteEndpoint
      .withUserId()
      .toRoutes { case ((_, taskId), userId) => taskDelete(taskId, userId) }

    projectCreateRoute <+>
      projectUpdateRoute <+>
      projectDeleteRoute <+>
      taskCreateRoute <+>
      taskUpdateRoute <+>
      taskDeleteRoute
  }

  private def projectCreate(input: ProjectInput, userId: UserId): F[Either[ApiError, LocationHeader]] =
    projectService
      .create(input, userId)
      .leftMap(mapToApiErrors)
      .map(_ => new LocationHeader(s"$gatewayAddress/projects/${input.projectId}"))
      .value

  private def projectUpdate(
    projectId: ProjectId,
    input: ProjectInput,
    userId: UserId
  ): F[Either[ApiError, LocationHeader]] =
    projectService
      .update(projectId, input, userId)
      .leftMap(mapToApiErrors)
      .map(_ => new LocationHeader(s"$gatewayAddress/projects/${input.projectId}"))
      .value

  private def projectDelete(projectId: ProjectId, userId: UserId): F[Either[ApiError, Unit]] =
    projectService
      .delete(projectId, userId)
      .leftMap(mapToApiErrors)
      .value

  private def taskAdd(projectId: ProjectId, input: TaskInput, userId: UserId): F[Either[ApiError, LocationHeader]] =
    taskService
      .add(projectId, input, userId)
      .leftMap(mapToApiErrors)
      .map(task => new LocationHeader(s"$gatewayAddress/projects/${projectId}/tasks/${task.taskId}"))
      .value

  private def taskUpdate(
    projectId: ProjectId,
    taskId: TaskId,
    input: TaskInput,
    userId: UserId
  ): F[Either[ApiError, LocationHeader]] =
    taskService
      .update(taskId, input, userId)
      .leftMap(mapToApiErrors)
      .map(task => new LocationHeader(s"$gatewayAddress/projects/${projectId}/tasks/${task.taskId}"))
      .value

  private def taskDelete(taskId: TaskId, userId: UserId): F[Either[ApiError, Unit]] =
    taskService
      .delete(taskId, userId)
      .leftMap(mapToApiErrors)
      .value

  implicit private class AuthorizedEndpoint[I, E, O, F[_]](e: Endpoint[I, E, O, EntityBody[F]]) {
    def withUserId(): Endpoint[(I, UserId), E, O, EntityBody[F]] = e.in(header[UserId]("X-Authorization"))
  }

  private def mapToApiErrors(error: AppFailure): ApiError =
    error match {
      case NotUnique(m)      => Conflict(m)
      case NotOwner(m)       => Forbidden(m)
      case EntityNotFound(m) => NotFound(m)
      case InvalidTimeSpan   => Conflict("other task overlaps task time span")
      case AlreadyDeleted(m) => Conflict(m)
    }
}
