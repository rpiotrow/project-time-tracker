package io.github.rpiotrow.ptt.read.web

import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.OffsetDateTime

import com.softwaremill.diffx.scalatest.DiffMatcher._
import eu.timepit.refined.auto._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.refined._
import io.github.rpiotrow.ptt.api.error.ServerError
import io.github.rpiotrow.ptt.api.model.ProjectId
import io.github.rpiotrow.ptt.api.output.{ProjectOutput, TaskOutput}
import io.github.rpiotrow.ptt.read.repository.{EntityNotFound, RepositoryThrowable}
import io.github.rpiotrow.ptt.read.service.ProjectService
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.scalatest.funspec.AnyFunSpec
import zio.Runtime.default.unsafeRun
import zio.Task
import zio.interop.catz._

class ProjectRoutesSpec extends AnyFunSpec with RoutesSpecBase {

  describe("valid parameters") {
    it(s"$projects/project-one") {
      val url = s"$projects/project-one"
      checkProject(url, "project-one")
    }
    it(s"$projects/ -- ") {
      val url = s"$projects/%20--%20"
      checkProject(url, " -- ")
    }
    it("proper JSON") {
      val url = s"$projects/project-one"
      checkJson(url, "project-one")
    }
    it(s"$projects/not-existing") {
      val projectService       = mock[ProjectService.Service]
      val projectId: ProjectId = "not-existing"
      (projectService.one _).expects(projectId).returning(zio.IO.fail(EntityNotFound(projectId.value)))

      val url      = s"$projects/not-existing"
      val response = makeRequest(Request(uri = Uri.unsafeFromString(url)), projectService)

      response.status should be(Status.NotFound)
    }
  }
  describe("service error") {
    it(s"$projects/project-one") {
      val url                  = s"$projects/project-one"
      val projectService       = mock[ProjectService.Service]
      val projectId: ProjectId = "project-one"
      (projectService.one _).expects(projectId).returning(zio.IO.fail(RepositoryThrowable(new RuntimeException())))

      val response = makeRequest(Request(uri = Uri.unsafeFromString(url)), projectService)
      val body     = unsafeRun(response.as[ServerError])

      response.status should be(Status.InternalServerError)
      body should be(ServerError("server.error"))
    }
  }

  private def project(id: ProjectId) = {
    val projectService = mock[ProjectService.Service]
    (projectService.one _).expects(id).returning(zio.IO.succeed(projectOutput1))
    projectService
  }

  private def checkProject(url: String, id: ProjectId) = {
    val response = makeRequest(Request(uri = Uri.unsafeFromString(url)), project(id))

    response.status should be(Status.Ok)
    bodyAsProjectOutput(response) should matchTo(projectOutput1)
  }

  private def checkJson(url: String, id: ProjectId) = {
    def taskToJsonObject(task: TaskOutput) = {
      Json.obj(
        "taskId"    -> Json.fromString(task.taskId.id.toString),
        "owner"     -> Json.fromString(task.owner.id.toString),
        "startedAt" -> Json.fromString(dateTimeString(task.startedAt)),
        "duration"  -> Json.fromString(task.duration.toString),
        "volume"    -> task.volume.map(Json.fromInt).getOrElse(Json.Null),
        "comment"   -> task.comment.map(Json.fromString).getOrElse(Json.Null),
        "deletedAt" -> task.deletedAt.map(e => Json.fromString(e.toString)).getOrElse(Json.Null)
      )
    }

    val response = makeRequest(Request(uri = Uri.unsafeFromString(url)), project(id))

    bodyAsJson(response) should be(
      Right(
        Json.obj(
          "projectId"   -> Json.fromString(projectOutput1.projectId.toString),
          "createdAt"   -> Json.fromString(dateTimeString(projectOutput1.createdAt)),
          "deletedAt"   -> Json.Null,
          "owner"       -> Json.fromString(projectOutput1.owner.id.toString),
          "durationSum" -> Json.fromString(projectOutput1.durationSum.toString),
          "tasks"       -> Json
            .fromValues(List(taskToJsonObject(projectOutput1.tasks(0)), taskToJsonObject(projectOutput1.tasks(1))))
        )
      )
    )
  }

  private def dateTimeString(OffsetDateTime: OffsetDateTime): String = OffsetDateTime.format(ISO_OFFSET_DATE_TIME)

  private def bodyAsProjectOutput(response: Response[Task]): ProjectOutput = {
    import io.github.rpiotrow.ptt.api.CirceMappings._
    unsafeRun(response.as[ProjectOutput])
  }

}
