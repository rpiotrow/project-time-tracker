package io.github.rpiotrow.ptt.write.web

import java.time.{Duration, LocalDateTime}

import cats.data.EitherT
import cats.effect.IO
import eu.timepit.refined.auto._
import fs2.Stream
import io.circe.Encoder._
import io.circe.generic.auto._
import io.circe.refined._
import io.circe.syntax._
import io.github.rpiotrow.ptt.api.input.ProjectInput
import io.github.rpiotrow.ptt.api.output.ProjectOutput
import io.github.rpiotrow.ptt.write.service.{NotUnique, ProjectService}
import org.http4s._
import org.http4s.headers.Location
import org.http4s.util.CaseInsensitiveString
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should
import sttp.model.HeaderNames

class ProjectCreateRouteSpec extends AnyFunSpec with RouteSpecBase with MockFactory with should.Matchers {

  describe("POST /projects") {
    it("successful") {
      val url      = s"/projects"
      val body     = Stream.evalSeq(IO { projectInput.asJson.toString().getBytes.toSeq })
      val response =
        makeRequest(Request(method = Method.POST, uri = Uri.unsafeFromString(url), body = body), project())

      response.status should be(Status.Created)
      response.headers.find(_.name == CaseInsensitiveString(HeaderNames.Location)) should be(
        Some(Location(Uri.unsafeFromString("http://gateway.live/projects/project1")))
      )
    }
    describe("failure") {
      it("when id is not unique") {
        val url      = s"/projects"
        val body     = Stream.evalSeq(IO { projectInput.asJson.toString().getBytes.toSeq })
        val response =
          makeRequest(Request(method = Method.POST, uri = Uri.unsafeFromString(url), body = body), noProject())

        response.status should be(Status.BadRequest)
        response.headers.find(_.name == CaseInsensitiveString(HeaderNames.Location)) should be(None)
      }
    }
  }

  private val projectInput  = ProjectInput(projectId = "project1")
  private val projectOutput = ProjectOutput(
    projectId = "project1",
    owner = ownerId,
    createdAt = LocalDateTime.now(),
    durationSum = Duration.ZERO,
    tasks = List()
  )

  private def project() = {
    val projectService = mock[ProjectService]
    (projectService.create _).expects(projectInput, ownerId).returning(EitherT.right(IO(projectOutput)))
    projectService
  }
  private def noProject() = {
    val projectService = mock[ProjectService]
    (projectService.create _)
      .expects(projectInput, ownerId)
      .returning(EitherT.left(IO(NotUnique("project id is not unique"))))
    projectService
  }

}
