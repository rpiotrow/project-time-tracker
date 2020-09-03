package io.github.rpiotrow.ptt.write.web

import java.util.UUID

import eu.timepit.refined.auto._
import cats.data.EitherT
import cats.effect.IO
import io.github.rpiotrow.ptt.api.model.{ProjectId, TaskId}
import io.github.rpiotrow.ptt.write.service.{AlreadyDeleted, EntityNotFound, NotOwner, ProjectNotMatch, TaskService}
import org.http4s._
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class TaskDeleteRouteSpec extends AnyFunSpec with RouteSpecBase with MockFactory with should.Matchers {

  private val taskId: TaskId       = TaskId.random()
  private val projectId: ProjectId = "bad"
  private val url                  = s"/projects/$projectId/tasks/${taskId.id}"

  describe(s"DELETE $url") {
    it("successful") {
      val taskService = mock[TaskService[IO]]
      (taskService.delete _).expects(projectId, taskId, ownerId).returning(EitherT.right(IO(())))
      val response    = makeDeleteTaskRequest(taskService)

      response.status should be(Status.Ok)
    }
    describe("failure") {
      it("when task does not exist") {
        val taskService = mock[TaskService[IO]]
        (taskService.delete _)
          .expects(projectId, taskId, ownerId)
          .returning(EitherT.left(IO(EntityNotFound("task not exist"))))
        val response    = makeDeleteTaskRequest(taskService)

        response.status should be(Status.NotFound)
      }
      it("when owner does not match authorized user") {
        val taskService = mock[TaskService[IO]]
        (taskService.delete _)
          .expects(projectId, taskId, ownerId)
          .returning(EitherT.left(IO(NotOwner("only owner can delete task"))))
        val response    = makeDeleteTaskRequest(taskService)

        response.status should be(Status.Forbidden)
      }
      it("when project id does not match") {
        val taskService = mock[TaskService[IO]]
        (taskService.delete _)
          .expects(projectId, taskId, ownerId)
          .returning(EitherT.left(IO(ProjectNotMatch("project does not match"))))
        val response    = makeDeleteTaskRequest(taskService)

        response.status should be(Status.NotFound)
      }
      it("when task is already deleted") {
        val taskService = mock[TaskService[IO]]
        (taskService.delete _)
          .expects(projectId, taskId, ownerId)
          .returning(EitherT.left(IO(AlreadyDeleted("task was already deleted"))))
        val response    = makeDeleteTaskRequest(taskService)

        response.status should be(Status.Conflict)
      }
    }
  }

  private def makeDeleteTaskRequest(taskService: TaskService[IO]) = {
    makeRequest(Request(method = Method.DELETE, uri = Uri.unsafeFromString(url)), taskService = taskService)
  }

}
