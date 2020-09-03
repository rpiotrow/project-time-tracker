package io.github.rpiotrow.ptt.write.web

import java.util.UUID

import cats.effect.{ContextShift, IO}
import io.github.rpiotrow.ptt.api.model.UserId
import io.github.rpiotrow.ptt.write.service.{ProjectService, TaskService}
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Header, Request, Response}
import org.scalamock.scalatest.MockFactory

trait RouteSpecBase { this: MockFactory =>

  implicit protected val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  private val ownerIdUUID       = "8fd49eff-3fcb-461d-bb85-9f5244286411"
  protected val ownerId: UserId = UserId(ownerIdUUID)

  protected def makeRequest(
    request: Request[IO],
    projectService: ProjectService[IO] = mock[ProjectService[IO]],
    taskService: TaskService[IO] = mock[TaskService[IO]]
  ): Response[IO] = {
    val requestWithAuth = request.withHeaders(Header.Raw(CaseInsensitiveString("X-Authorization"), ownerIdUUID))
    val routes          = Routes.test("http://gateway.live", projectService, taskService)

    routes
      .writeSideRoutes()
      .run(requestWithAuth)
      .value
      .map(_.getOrElse(Response.notFound))
      .unsafeRunSync()
  }
}
