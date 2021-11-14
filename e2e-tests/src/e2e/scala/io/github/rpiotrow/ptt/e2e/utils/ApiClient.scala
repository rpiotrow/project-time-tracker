package io.github.rpiotrow.ptt.e2e.utils

import io.github.rpiotrow.ptt.api.ProjectEndpoints._
import io.github.rpiotrow.ptt.api.StatisticsEndpoints.statisticsEndpoint
import io.github.rpiotrow.ptt.api.TaskEndpoints._
import io.github.rpiotrow.ptt.api.error
import io.github.rpiotrow.ptt.api.input._
import io.github.rpiotrow.ptt.api.model._
import io.github.rpiotrow.ptt.api.output._
import io.github.rpiotrow.ptt.api.param._
import io.github.rpiotrow.ptt.e2e.configuration.End2EndTestsConfiguration.config
import io.github.rpiotrow.ptt.e2e.factories.AuthorizationTokenFactory._
import org.scalatest.matchers.should
import sttp.client3._
import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.tapir.codec.refined._
import sttp.tapir._

object ApiClient extends should.Matchers {

  private val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()

  def projectList(projectListParams: ProjectListParams, userId: UserId): Either[error.ApiError, List[ProjectOutput]] =
    requestWithAuth(projectListEndpoint, projectListParams, userId)
  def projectDetail(projectId: ProjectId, userId: UserId): Either[error.ApiError, ProjectOutput]                     =
    requestWithAuth(projectDetailEndpoint, projectId, userId)

  def createProject(projectInput: ProjectInput, userId: UserId): Either[error.ApiError, LocationHeader] =
    requestWithAuth(projectCreateEndpoint, projectInput, userId)
  def updateProject(
    projectId: ProjectId,
    projectInput: ProjectInput,
    userId: UserId
  ): Either[error.ApiError, LocationHeader]                                                             =
    requestWithAuth(projectUpdateEndpoint, (projectId, projectInput), userId)
  def deleteProject(projectId: ProjectId, userId: UserId): Either[error.ApiError, Unit]                 =
    requestWithAuth(projectDeleteEndpoint, projectId, userId)

  def createTask(projectId: ProjectId, taskInput: TaskInput, userId: UserId): Either[error.ApiError, LocationHeader] =
    requestWithAuth(taskCreateEndpoint, (projectId, taskInput), userId)
  def updateTask(
    projectId: ProjectId,
    taskId: TaskId,
    taskInput: TaskInput,
    userId: UserId
  ): Either[error.ApiError, LocationHeader]                                                                          =
    requestWithAuth(taskUpdateEndpoint, (projectId, taskId, taskInput), userId)
  def deleteTask(projectId: ProjectId, taskId: TaskId, userId: UserId): Either[error.ApiError, Unit]                 =
    requestWithAuth(taskDeleteEndpoint, (projectId, taskId), userId)

  def statistics(statisticsParams: StatisticsParams, userId: UserId): Either[error.ApiError, StatisticsOutput] =
    requestWithAuth(statisticsEndpoint, statisticsParams, userId)

  private def requestWithAuth[I, E, O](e: Endpoint[I, E, O, Any], input: I, userId: UserId) = {
    val endpointWithAuth = e.in(auth.bearer[BearerToken]())
    val request          = SttpClientInterpreter().toRequest(endpointWithAuth, Some(uri"${config.application.baseUri}"))
    val inputWithToken   = (input, generateValidToken(userId))
    request(inputWithToken).send(backend).body match {
      case DecodeResult.Value(v) => v
      case e                     => fail("request failed: " + e)
    }
  }

}
