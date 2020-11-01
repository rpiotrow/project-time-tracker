package io.github.rpiotrow.ptt.write.web

import java.time.{Duration, OffsetDateTime}
import java.util.UUID

import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import eu.timepit.refined.auto._
import fs2.Stream
import io.circe.Encoder._
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.rpiotrow.ptt.api.input.TaskInput
import io.github.rpiotrow.ptt.api.model.{ProjectId, TaskId}
import io.github.rpiotrow.ptt.api.output.TaskOutput
import io.github.rpiotrow.ptt.write.service._
import org.http4s.headers.Location
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Method, Request, Status, Uri}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should
import sttp.model.HeaderNames

class TaskUpdateRouteSpec extends AnyFunSpec with RouteSpecBase with MockFactory with should.Matchers {

  private val projectId: ProjectId = "pp3"
  private val taskId: TaskId       = TaskId.random()
  private val newTaskIdUUID        = UUID.randomUUID()
  private val newTaskId: TaskId    = TaskId(newTaskIdUUID)

  describe(s"PUT /projects/$projectId/tasks/${taskId.id}") {
    it("successful") {
      val taskService = mock[TaskService[IO]]
      (taskService.update _).expects(projectId, taskId, taskInput, ownerId).returning(EitherT.right(IO(newTaskId)))
      val response    = makeUpdateTaskRequest(taskService)

      response.status should be(Status.Ok)
      response.headers.find(_.name == CaseInsensitiveString(HeaderNames.Location)) should be(
        Some(Location(Uri.unsafeFromString(s"http://gateway.live/projects/pp3/tasks/$newTaskIdUUID")))
      )
    }
    describe("failure") {
      it("when project does not exist") {
        val taskService = mock[TaskService[IO]]
        (taskService.update _)
          .expects(projectId, taskId, taskInput, ownerId)
          .returning(EitherT.left(IO(EntityNotFound("project with given projectId does not exist"))))
        val response    = makeUpdateTaskRequest(taskService)

        response.status should be(Status.NotFound)
        response.headers.find(_.name == CaseInsensitiveString(HeaderNames.Location)) should be(None)
      }
      it("when owner does not match authorized user") {
        val taskService = mock[TaskService[IO]]
        (taskService.update _)
          .expects(projectId, taskId, taskInput, ownerId)
          .returning(EitherT.left(IO(NotOwner("only owner can update task"))))
        val response    = makeUpdateTaskRequest(taskService)

        response.status should be(Status.Forbidden)
      }
      it("when project id does not match") {
        val taskService = mock[TaskService[IO]]
        (taskService.update _)
          .expects(projectId, taskId, taskInput, ownerId)
          .returning(EitherT.left(IO(ProjectNotMatch("project does not match"))))
        val response    = makeUpdateTaskRequest(taskService)

        response.status should be(Status.NotFound)
      }
      it("when task time span is invalid") {
        val taskService            = mock[TaskService[IO]]
        val appFailure: AppFailure = InvalidTimeSpan
        (taskService.update _)
          .expects(projectId, taskId, taskInput, ownerId)
          .returning(EitherT.left(IO(appFailure)))
        val response               = makeUpdateTaskRequest(taskService)

        response.status should be(Status.Conflict)
        response.headers.find(_.name == CaseInsensitiveString(HeaderNames.Location)) should be(None)
      }
      it("when task is deleted") {
        val taskService = mock[TaskService[IO]]
        (taskService.update _)
          .expects(projectId, taskId, taskInput, ownerId)
          .returning(EitherT.left(IO(AlreadyDeleted("task was deleted"))))
        val response    = makeUpdateTaskRequest(taskService)

        response.status should be(Status.Conflict)
        response.headers.find(_.name == CaseInsensitiveString(HeaderNames.Location)) should be(None)
      }
    }
  }

  private val taskInput  = TaskInput(
    startedAt = OffsetDateTime.now(),
    duration = Duration.ofMinutes(30),
    volume = 10.some,
    comment = "text".some
  )
  private val taskOutput = TaskOutput(
    taskId = newTaskId,
    owner = ownerId,
    startedAt = OffsetDateTime.now(),
    duration = Duration.ofMinutes(30),
    volume = 10.some,
    comment = "text".some,
    deletedAt = None
  )

  private def makeUpdateTaskRequest(taskService: TaskService[IO]) = {
    val url  = s"/projects/$projectId/tasks/${taskId.id}"
    val body = Stream.evalSeq(IO { taskInput.asJson.toString().getBytes.toSeq })
    makeRequest(
      request = Request(method = Method.PUT, uri = Uri.unsafeFromString(url), body = body),
      taskService = taskService
    )
  }

}
