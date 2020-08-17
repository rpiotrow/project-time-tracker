package io.github.rpiotrow.ptt.write.web

import java.util.UUID

import cats.effect.{ContextShift, IO}
import io.github.rpiotrow.ptt.write.service.{ProjectService, TaskService}
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Header, Request, Response}
import org.scalamock.scalatest.MockFactory

trait RouteSpecBase { this: MockFactory =>

  implicit protected val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  protected val ownerId: UUID = UUID.randomUUID()

  protected def makeRequest(
    request: Request[IO],
    projectService: ProjectService = mock[ProjectService],
    taskService: TaskService = mock[TaskService]
  ): Response[IO] = {
    val requestWithAuth = request.withHeaders(Header.Raw(CaseInsensitiveString("X-Authorization"), ownerId.toString))
    val routes          = Routes.test("http://gateway.live", projectService, taskService)

    routes
      .writeSideRoutes()
      .run(requestWithAuth)
      .value
      .map(_.getOrElse(Response.notFound))
      .unsafeRunSync()
  }
}
