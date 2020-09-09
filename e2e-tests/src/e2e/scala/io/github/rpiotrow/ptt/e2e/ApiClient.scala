package io.github.rpiotrow.ptt.e2e

import io.github.rpiotrow.ptt.api.ProjectEndpoints._
import io.github.rpiotrow.ptt.api.StatisticsEndpoints._
import io.github.rpiotrow.ptt.api.TaskEndpoints._
import io.github.rpiotrow.ptt.api.error
import io.github.rpiotrow.ptt.api.input._
import io.github.rpiotrow.ptt.api.model._
import io.github.rpiotrow.ptt.api.output._
import io.github.rpiotrow.ptt.api.param._
import io.github.rpiotrow.ptt.e2e.configuration.End2EndTestsConfiguration.config
import io.github.rpiotrow.ptt.e2e.factories.AuthorizationTokenFactory.generateValidToken
import io.github.rpiotrow.ptt.e2e.factories.UserIdFactory.generateUserId
import org.scalatest.matchers.should
import sttp.client._
import sttp.tapir._
import sttp.tapir.client.sttp._
import sttp.tapir.codec.refined._

object ApiClient extends should.Matchers {

  private val testToken: BearerToken = generateValidToken(generateUserId())

  implicit private val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

  def projectList(projectListParams: ProjectListParams): Either[error.ApiError, List[ProjectOutput]] =
    requestWithAuth(projectListEndpoint, projectListParams)
  def projectDetail(projectId: ProjectId): Either[error.ApiError, ProjectOutput]                     =
    requestWithAuth(projectDetailEndpoint, projectId)

  def createProject(projectInput: ProjectInput): Either[error.ApiError, LocationHeader]                       =
    requestWithAuth(projectCreateEndpoint, projectInput)
  def updateProject(projectId: ProjectId, projectInput: ProjectInput): Either[error.ApiError, LocationHeader] =
    requestWithAuth(projectUpdateEndpoint, (projectId, projectInput))
  def deleteProject(projectId: ProjectId): Either[error.ApiError, Unit]                                       =
    requestWithAuth(projectDeleteEndpoint, projectId)

  def createTask(projectId: ProjectId, taskInput: TaskInput): Either[error.ApiError, LocationHeader]                 =
    requestWithAuth(taskCreateEndpoint, (projectId, taskInput))
  def updateTask(projectId: ProjectId, taskId: TaskId, taskInput: TaskInput): Either[error.ApiError, LocationHeader] =
    requestWithAuth(taskUpdateEndpoint, (projectId, taskId, taskInput))
  def deleteTask(projectId: ProjectId, taskId: TaskId): Either[error.ApiError, Unit]                                 =
    requestWithAuth(taskDeleteEndpoint, (projectId, taskId))

  def statistics(statisticsParams: StatisticsParams): Either[error.ApiError, StatisticsOutput] =
    requestWithAuth(statisticsEndpoint, statisticsParams)

  private def requestWithAuth[I, E, O](e: Endpoint[I, E, O, Nothing], input: I) = {
    val request        = e.in(auth.bearer[BearerToken]).toSttpRequest(uri"${config.application.baseUri}")
    val inputWithToken = (input, testToken)
    request(inputWithToken).send().body match {
      case DecodeResult.Value(v) => v
      case e                     => fail("request failed: " + e)
    }
  }

}
