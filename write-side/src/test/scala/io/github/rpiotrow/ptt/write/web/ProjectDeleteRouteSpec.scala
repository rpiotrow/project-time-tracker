package io.github.rpiotrow.ptt.write.web

import cats.data.EitherT
import cats.effect.IO
import eu.timepit.refined.auto._
import io.github.rpiotrow.ptt.api.model.ProjectId
import io.github.rpiotrow.ptt.write.service.{AlreadyDeleted, EntityNotFound, NotOwner, ProjectService}
import org.http4s._
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class ProjectDeleteRouteSpec extends AnyFunSpec with RouteSpecBase with MockFactory with should.Matchers {

  describe("DELETE /projects/one") {
    it("successful") {
      val projectId: ProjectId = "one"
      val projectService       = mock[ProjectService[IO]]
      (projectService.delete _).expects(projectId, ownerId).returning(EitherT.right(IO(())))
      val response             = makeDeleteProjectRequest(projectId, projectService)

      response.status should be(Status.Ok)
    }
    describe("failure") {
      it("when project does not exist") {
        val projectId: ProjectId = "two"
        val projectService       = mock[ProjectService[IO]]
        (projectService.delete _)
          .expects(projectId, ownerId)
          .returning(EitherT.left(IO(EntityNotFound("project with given projectId does not exist"))))
        val response             = makeDeleteProjectRequest(projectId, projectService)

        response.status should be(Status.NotFound)
      }
      it("when owner does not match authorized user") {
        val projectId: ProjectId = "three"
        val projectService       = mock[ProjectService[IO]]
        (projectService.delete _)
          .expects(projectId, ownerId)
          .returning(EitherT.left(IO(NotOwner("only owner can delete project"))))
        val response             = makeDeleteProjectRequest(projectId, projectService)

        response.status should be(Status.Forbidden)
      }
      it("when project is already deleted") {
        val projectId: ProjectId = "three"
        val projectService       = mock[ProjectService[IO]]
        (projectService.delete _)
          .expects(projectId, ownerId)
          .returning(EitherT.left(IO(AlreadyDeleted("project was already deleted"))))
        val response             = makeDeleteProjectRequest(projectId, projectService)

        response.status should be(Status.Conflict)
      }
    }
  }

  private def makeDeleteProjectRequest(projectId: ProjectId, projectService: ProjectService[IO]) = {
    val url = s"/projects/$projectId"
    makeRequest(Request(method = Method.DELETE, uri = Uri.unsafeFromString(url)), projectService)
  }

}
