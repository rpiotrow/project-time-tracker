package io.github.rpiotrow.ptt.write.web

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.github.rpiotrow.ptt.api.model.UserId
import io.github.rpiotrow.ptt.write.service.{ProjectService, TaskService}
import org.http4s.{Header, Request, Response}
import org.scalamock.scalatest.MockFactory
import org.typelevel.ci.CIString

trait RouteSpecBase { this: MockFactory =>

  private val ownerIdUUID       = "8fd49eff-3fcb-461d-bb85-9f5244286411"
  protected val ownerId: UserId = UserId(ownerIdUUID)

  protected def makeRequest(
    request: Request[IO],
    projectService: ProjectService[IO] = mock[ProjectService[IO]],
    taskService: TaskService[IO] = mock[TaskService[IO]]
  ): Response[IO] = {
    val requestWithAuth = request.withHeaders(Header.Raw(CIString("X-Authorization"), ownerIdUUID))
    val routes          = Routes.test("http://gateway.live", projectService, taskService)

    routes
      .writeSideRoutes()
      .run(requestWithAuth)
      .value
      .map(_.getOrElse(Response.notFound))
      .unsafeRunSync()
  }
}
