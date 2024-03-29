package io.github.rpiotrow.ptt.write.web

import cats.Monad
import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import eu.timepit.refined.auto._
import fs2.Stream
import io.circe.Encoder._
import io.circe.generic.auto._
import io.circe.refined._
import io.circe.syntax._
import io.github.rpiotrow.ptt.api.input.ProjectInput
import io.github.rpiotrow.ptt.api.model.ProjectId
import io.github.rpiotrow.ptt.write.service.{EntityNotFound, NotOwner, NotUnique, ProjectService}
import org.http4s._
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should
import org.typelevel.ci.CIString
import sttp.model.HeaderNames

class ProjectUpdateRouteSpec extends AnyFunSpec with RouteSpecBase with MockFactory with should.Matchers {

  private val projectId: ProjectId = "change-me"
  private val projectInput         = ProjectInput(projectId = "project-new")

  describe("PUT /projects/change-me") {
    it("successful") {
      val projectService = mock[ProjectService[IO]]
      (projectService.update _).expects(projectId, projectInput, ownerId).returning(EitherT.right(Monad[IO].unit))
      val response       = makeUpdateProjectRequest(projectService)

      response.status should be(Status.Ok)
      response.headers.get(CIString(HeaderNames.Location)) should be(
        Some(NonEmptyList.of(
          Header.Raw(CIString("Location"), "http://gateway.live/projects/project-new")
        ))
      )
    }
    describe("failure") {
      it("when id is not unique") {
        val projectService = mock[ProjectService[IO]]
        (projectService.update _)
          .expects(projectId, projectInput, ownerId)
          .returning(EitherT.left(IO(NotUnique("project id is not unique"))))
        val response       = makeUpdateProjectRequest(projectService)

        response.status should be(Status.Conflict)
        response.headers.get(CIString(HeaderNames.Location)) should be(None)
      }
      it("when user is not owner of the project") {
        val projectService = mock[ProjectService[IO]]
        (projectService.update _)
          .expects(projectId, projectInput, ownerId)
          .returning(EitherT.left(IO(NotOwner("project is not owned by invoking user"))))
        val response       = makeUpdateProjectRequest(projectService)

        response.status should be(Status.Forbidden)
        response.headers.get(CIString(HeaderNames.Location)) should be(None)
      }
      it("when project does not exist") {
        val projectService = mock[ProjectService[IO]]
        (projectService.update _)
          .expects(projectId, projectInput, ownerId)
          .returning(EitherT.left(IO(EntityNotFound("project with given identifier does not exist"))))
        val response       = makeUpdateProjectRequest(projectService)

        response.status should be(Status.NotFound)
        response.headers.get(CIString(HeaderNames.Location)) should be(None)
      }
    }
  }

  private def makeUpdateProjectRequest(projectService: ProjectService[IO]) = {
    val url  = "/projects/change-me"
    val body = Stream.evalSeq(IO { projectInput.asJson.toString().getBytes.toSeq })
    makeRequest(Request(method = Method.PUT, uri = Uri.unsafeFromString(url), body = body), projectService)
  }

}
