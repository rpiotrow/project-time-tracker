package io.github.rpiotrow.ptt.api.write.web

import java.time.{Duration, LocalDateTime}
import java.util.UUID

import eu.timepit.refined.auto._
import io.circe.generic.auto._
import io.circe.refined._
import io.circe.syntax._
import io.github.rpiotrow.ptt.api.input.ProjectInput
import io.github.rpiotrow.ptt.api.output.ProjectOutput
import io.github.rpiotrow.ptt.api.write.configuration.GatewayConfiguration
import io.github.rpiotrow.ptt.api.write.service.{NotUnique, ProjectService}
import org.http4s._
import org.http4s.headers.Location
import org.http4s.util.CaseInsensitiveString
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should
import sttp.model.HeaderNames
import zio.Runtime.default.unsafeRun
import zio._
import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor.descriptor

class ProjectCreateRouteSpec extends AnyFunSpec with MockFactory with should.Matchers {
  describe("POST /projects") {
    it("successful") {
      val url      = s"/projects"
      val body     = fs2.Stream.evalSeq(zio.Task { projectInput.asJson.toString().getBytes.toSeq })
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
        val body     = fs2.Stream.evalSeq(zio.Task { projectInput.asJson.toString().getBytes.toSeq })
        val response =
          makeRequest(Request(method = Method.POST, uri = Uri.unsafeFromString(url), body = body), noProject())

        response.status should be(Status.BadRequest)
        response.headers.find(_.name == CaseInsensitiveString(HeaderNames.Location)) should be(None)
      }
    }
  }

  private val ownerId: UUID = UUID.randomUUID()
  private val projectInput  = ProjectInput(projectId = "project1")
  private val projectOutput = ProjectOutput(
    id = "project1",
    owner = ownerId,
    createdAt = LocalDateTime.now(),
    durationSum = Duration.ZERO,
    tasks = List()
  )

  private def project() = {
    val projectService = mock[ProjectService.Service]
    (projectService.create _).expects(projectInput, ownerId).returning(zio.IO.succeed(projectOutput))
    projectService
  }
  private def noProject() = {
    val projectService = mock[ProjectService.Service]
    (projectService.create _)
      .expects(projectInput, ownerId)
      .returning(zio.IO.fail(NotUnique("project id is not unique")))
    projectService
  }

  private def makeRequest(
    request: Request[Task],
    projectService: ProjectService.Service = mock[ProjectService.Service]
  ): Response[Task] = {
    val requestWithAuth = request.withHeaders(Header.Raw(CaseInsensitiveString("X-Authorization"), ownerId.toString))
    val app             = for {
      routes   <- Routes.readSideRoutes()
      response <- routes.run(requestWithAuth).value
    } yield response.getOrElse(Response.notFound)

    val services =
      ZLayer.fromFunction[Any, ProjectService.Service](_ => projectService)
    val config   =
      ZConfig.fromMap(Map("address" -> "http://gateway.live"), descriptor[GatewayConfiguration])
    unsafeRun(app.provideLayer(services ++ config >>> Routes.live))
  }

}
