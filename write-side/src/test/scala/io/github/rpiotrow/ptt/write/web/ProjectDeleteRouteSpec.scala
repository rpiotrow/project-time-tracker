package io.github.rpiotrow.ptt.write.web

import java.util.UUID

import cats.data.EitherT
import cats.effect.IO
import eu.timepit.refined.auto._
import io.github.rpiotrow.ptt.api.model.ProjectId
import io.github.rpiotrow.ptt.write.service.{EntityNotFound, NotOwner, ProjectService}
import org.http4s._
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class ProjectDeleteRouteSpec extends AnyFunSpec with RouteSpecBase with MockFactory with should.Matchers {

  describe("DELETE /projects/one") {
    it("successful") {
      val url      = "/projects/one"
      val response =
        makeRequest(Request(method = Method.DELETE, uri = Uri.unsafeFromString(url)), project("one"))

      response.status should be(Status.Ok)
    }
    describe("failure") {
      it("when project does not exist") {
        val url      = "/projects/two"
        val response =
          makeRequest(Request(method = Method.DELETE, uri = Uri.unsafeFromString(url)), noProject("two"))

        response.status should be(Status.NotFound)
      }
      it("when owner does not match authorized user") {
        val url      = "/projects/three"
        val someUser = UUID.randomUUID()
        val response =
          makeRequest(
            request = Request(method = Method.DELETE, uri = Uri.unsafeFromString(url)),
            projectService = noOwnedProject("three")
          )

        response.status should be(Status.Forbidden)
      }
    }
  }

  private def project(projectId: ProjectId) = {
    val projectService = mock[ProjectService]
    (projectService.delete _).expects(projectId, ownerId).returning(EitherT.right(IO(())))
    projectService
  }
  private def noProject(projectId: ProjectId) = {
    val projectService = mock[ProjectService]
    (projectService.delete _)
      .expects(projectId, ownerId)
      .returning(EitherT.left(IO(EntityNotFound("project with given projectId does not exist"))))
    projectService
  }
  private def noOwnedProject(projectId: ProjectId) = {
    val projectService = mock[ProjectService]
    (projectService.delete _)
      .expects(projectId, ownerId)
      .returning(EitherT.left(IO(NotOwner("only owner can delete project"))))
    projectService
  }

}
