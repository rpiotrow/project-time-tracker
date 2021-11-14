package io.github.rpiotrow.ptt.write.web

import cats.implicits._
import cats.effect.{Async, Resource}
import io.github.rpiotrow.ptt.api.ProjectEndpoints._
import io.github.rpiotrow.ptt.api.TaskEndpoints._
import io.github.rpiotrow.ptt.api.error._
import io.github.rpiotrow.ptt.api.input._
import io.github.rpiotrow.ptt.api.model._
import io.github.rpiotrow.ptt.api.output._
import io.github.rpiotrow.ptt.write.configuration.AppConfiguration
import io.github.rpiotrow.ptt.write.service._
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._
import sttp.tapir.server.http4s.Http4sServerOptions.Log
import sttp.tapir.server.interceptor.exception.DefaultExceptionHandler
import sttp.tapir.server.interceptor.reject.RejectInterceptor
import sttp.tapir._

trait Routes[F[_]] {
  def writeSideRoutes(): HttpRoutes[F]
}

object Routes {

  def live[F[_]: Async](): Resource[F, Routes[F]] = {
    val gatewayConfiguration = AppConfiguration.live.gatewayConfiguration
    services[F]().map({
      case (projectService, taskService) =>
        new RoutesLive[F](gatewayConfiguration.address, projectService, taskService)
    })
  }

  def test[F[_]: Async](
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
) extends Routes[F] {

  private val serverOptions: Http4sServerOptions[F, F] =
    Http4sServerOptions.customInterceptors(
      Some(RejectInterceptor.default), Some(DefaultExceptionHandler), Some(Log.defaultServerLog),
      decodeFailureHandler = DecodeFailure.decodeFailureHandler
    )

  override def writeSideRoutes(): HttpRoutes[F] = {
    val interpreter = Http4sServerInterpreter[F](serverOptions)

    interpreter.toRoutes(projectCreateEndpoint.withUserId()) {
      case (params, userId) => projectCreate(params, userId)
    } <+> interpreter.toRoutes(projectUpdateEndpoint.withUserId()) {
      case ((projectId, input), userId) => projectUpdate(projectId, input, userId)
    } <+> interpreter.toRoutes(projectDeleteEndpoint.withUserId()) {
      case (projectId, userId) => projectDelete(projectId, userId)
    } <+> interpreter.toRoutes(taskCreateEndpoint.withUserId()) {
      case ((projectId, taskInput), userId) => taskAdd(projectId, taskInput, userId)
    } <+> interpreter.toRoutes(taskUpdateEndpoint.withUserId()) {
      case ((projectId, taskId, taskInput), userId) => taskUpdate(projectId, taskId, taskInput, userId)
    } <+> interpreter.toRoutes(taskDeleteEndpoint.withUserId()) {
      case ((projectId, taskId), userId) => taskDelete(projectId, taskId, userId)
    }
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
      .map(taskId => new LocationHeader(s"$gatewayAddress/projects/$projectId/tasks/${taskId.id}"))
      .value

  private def taskUpdate(
    projectId: ProjectId,
    taskId: TaskId,
    input: TaskInput,
    userId: UserId
  ): F[Either[ApiError, LocationHeader]] =
    taskService
      .update(projectId, taskId, input, userId)
      .leftMap(mapToApiErrors)
      .map(taskId => new LocationHeader(s"$gatewayAddress/projects/$projectId/tasks/${taskId.id}"))
      .value

  private def taskDelete(projectId: ProjectId, taskId: TaskId, userId: UserId): F[Either[ApiError, Unit]] =
    taskService
      .delete(projectId, taskId, userId)
      .leftMap(mapToApiErrors)
      .value

  import io.github.rpiotrow.ptt.api.TapirMappings.userIdCodec
  implicit private class AuthorizedEndpoint[I, E, O, -R](e: Endpoint[I, E, O, R]) {
    def withUserId(): Endpoint[(I, UserId), E, O, R] = e.in(header[UserId]("X-Authorization"))
  }

  private def mapToApiErrors(error: AppFailure): ApiError =
    error match {
      case NotUnique(m)       => Conflict(m)
      case NotOwner(m)        => Forbidden(m)
      case EntityNotFound(m)  => NotFound(m)
      case ProjectNotMatch(m) => NotFound(m)
      case InvalidTimeSpan    => Conflict("other task overlaps task time span")
      case AlreadyDeleted(m)  => Conflict(m)
    }
}
