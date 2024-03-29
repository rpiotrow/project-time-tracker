package io.github.rpiotrow.ptt.write.web

import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import eu.timepit.refined.auto._
import fs2.Stream
import io.circe.Encoder._
import io.circe.generic.auto._
import io.circe.refined._
import io.circe.syntax._
import io.github.rpiotrow.ptt.api.input.ProjectInput
import io.github.rpiotrow.ptt.write.service.{NotUnique, ProjectService}
import org.http4s._
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should
import org.typelevel.ci.CIString
import sttp.model.HeaderNames

class ProjectCreateRouteSpec extends AnyFunSpec with RouteSpecBase with MockFactory with should.Matchers {

  private val projectInput = ProjectInput(projectId = "project1")

  describe("POST /projects") {
    it("successful") {
      val projectService = mock[ProjectService[IO]]
      (projectService.create _).expects(projectInput, ownerId).returning(EitherT.right(IO.unit))
      val response       = makeAddProjectRequest(projectService)

      response.status should be(Status.Created)
      response.headers.get(CIString(HeaderNames.Location)) should be(
        Some(NonEmptyList.of(
          Header.Raw(CIString("Location"), "http://gateway.live/projects/project1")
        ))
      )
    }
    describe("failure") {
      it("when id is not unique") {
        val projectService = mock[ProjectService[IO]]
        (projectService.create _)
          .expects(projectInput, ownerId)
          .returning(EitherT.left(IO(NotUnique("project id is not unique"))))
        val response       = makeAddProjectRequest(projectService)

        response.status should be(Status.Conflict)
        response.headers.get(CIString(HeaderNames.Location)) should be(None)
      }
    }
  }

  private def makeAddProjectRequest(projectService: ProjectService[IO]) = {
    val url  = "/projects"
    val body = Stream.evalSeq(IO { projectInput.asJson.toString().getBytes.toSeq })
    makeRequest(Request(method = Method.POST, uri = Uri.unsafeFromString(url), body = body), projectService)
  }

}
