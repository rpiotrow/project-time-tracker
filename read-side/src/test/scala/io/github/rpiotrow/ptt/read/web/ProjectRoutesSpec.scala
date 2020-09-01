package io.github.rpiotrow.ptt.read.web

import eu.timepit.refined.auto._
import io.circe.generic.auto._
import io.github.rpiotrow.ptt.api.error.ServerError
import io.github.rpiotrow.ptt.api.model.ProjectId
import io.github.rpiotrow.ptt.api.output.ProjectOutput
import io.github.rpiotrow.ptt.read.repository.{EntityNotFound, RepositoryThrowable}
import io.github.rpiotrow.ptt.read.service.ProjectService
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.scalatest.funspec.AnyFunSpec
import zio.Runtime.default.unsafeRun
import zio.Task
import zio.interop.catz._

class ProjectRoutesSpec extends AnyFunSpec with RoutesSpecBase {

  private def project(id: ProjectId) = {
    val projectService = mock[ProjectService.Service]
    (projectService.one _).expects(id).returning(zio.IO.succeed(projectOutput1))
    projectService
  }

  private def checkProject(url: String, id: ProjectId) = {
    val response = makeRequest(Request(uri = Uri.unsafeFromString(url)), project(id))

    response.status should be(Status.Ok)
    body(response) should be(projectOutput1)
  }

  private def body(response: Response[Task]) = {
    unsafeRun(response.as[ProjectOutput])
  }

  describe("valid parameters") {
    it(s"$projects/project-one") {
      val url = s"$projects/project-one"
      checkProject(url, "project-one")
    }
    it(s"$projects/ -- ") {
      val url = s"$projects/%20--%20"
      checkProject(url, " -- ")
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

}
