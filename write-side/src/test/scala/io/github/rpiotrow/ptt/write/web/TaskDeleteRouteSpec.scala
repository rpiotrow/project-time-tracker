package io.github.rpiotrow.ptt.write.web

import java.util.UUID

import cats.data.EitherT
import cats.effect.IO
import io.github.rpiotrow.ptt.api.model.TaskId
import io.github.rpiotrow.ptt.write.service.{EntityNotFound, NotOwner, TaskService}
import org.http4s._
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class TaskDeleteRouteSpec extends AnyFunSpec with RouteSpecBase with MockFactory with should.Matchers {

  private val taskId: TaskId = UUID.fromString("ea48f025-d993-4052-8308-47db35d7ada0")
  private val url            = s"/projects/bad/tasks/$taskId"

  describe(s"DELETE $url") {
    it("successful") {
      val taskService = mock[TaskService]
      (taskService.delete _).expects(taskId, ownerId).returning(EitherT.right(IO(())))
      val response    = makeDeleteTaskRequest(taskId, taskService)

      response.status should be(Status.Ok)
    }
    describe("failure") {
      it("when task does not exist") {
        val taskService = mock[TaskService]
        (taskService.delete _)
          .expects(taskId, ownerId)
          .returning(EitherT.left(IO(EntityNotFound("task not exist"))))
        val response    = makeDeleteTaskRequest(taskId, taskService)

        response.status should be(Status.NotFound)
      }
      it("when owner does not match authorized user") {
        val taskService = mock[TaskService]
        (taskService.delete _)
          .expects(taskId, ownerId)
          .returning(EitherT.left(IO(NotOwner("only owner can delete task"))))
        val response    = makeDeleteTaskRequest(taskId, taskService)

        response.status should be(Status.Forbidden)
      }
    }
  }

  private def makeDeleteTaskRequest(projectId: TaskId, taskService: TaskService) = {
    makeRequest(Request(method = Method.DELETE, uri = Uri.unsafeFromString(url)), taskService = taskService)
  }

}
