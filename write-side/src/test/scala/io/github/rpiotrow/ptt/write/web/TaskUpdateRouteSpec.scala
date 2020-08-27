package io.github.rpiotrow.ptt.write.web

import java.time.{Duration, LocalDateTime}
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

  private val projectId: ProjectId = "pp1"
  private val taskId: TaskId       = UUID.randomUUID()
  private val newTaskId: TaskId    = UUID.randomUUID()

  describe(s"PUT /projects/$projectId/tasks/$taskId") {
    it("successful") {
      val taskService = mock[TaskService[IO]]
      (taskService.update _).expects(taskId, taskInput, ownerId).returning(EitherT.right(IO(taskOutput)))
      val response    = makeUpdateTaskRequest(taskService)

      response.status should be(Status.Ok)
      response.headers.find(_.name == CaseInsensitiveString(HeaderNames.Location)) should be(
        Some(Location(Uri.unsafeFromString(s"http://gateway.live/projects/$projectId/tasks/$newTaskId")))
      )
    }
    describe("failure") {
      it("when project does not exist") {
        val taskService = mock[TaskService[IO]]
        (taskService.update _)
          .expects(taskId, taskInput, ownerId)
          .returning(EitherT.left(IO(EntityNotFound("project with given projectId does not exist"))))
        val response    = makeUpdateTaskRequest(taskService)

        response.status should be(Status.NotFound)
        response.headers.find(_.name == CaseInsensitiveString(HeaderNames.Location)) should be(None)
      }
      it("when owner does not match authorized user") {
        val taskService = mock[TaskService[IO]]
        (taskService.update _)
          .expects(taskId, taskInput, ownerId)
          .returning(EitherT.left(IO(NotOwner("only owner can update task"))))
        val response    = makeUpdateTaskRequest(taskService)

        response.status should be(Status.Forbidden)
      }
      it("when task time span is invalid") {
        val taskService            = mock[TaskService[IO]]
        val appFailure: AppFailure = InvalidTimeSpan
        (taskService.update _)
          .expects(taskId, taskInput, ownerId)
          .returning(EitherT.left(IO(appFailure)))
        val response               = makeUpdateTaskRequest(taskService)

        response.status should be(Status.Conflict)
        response.headers.find(_.name == CaseInsensitiveString(HeaderNames.Location)) should be(None)
      }
    }
  }

  private val taskInput  = TaskInput(
    startedAt = LocalDateTime.now(),
    duration = Duration.ofMinutes(30),
    volume = 10.some,
    comment = "text".some
  )
  private val taskOutput = TaskOutput(
    taskId = newTaskId,
    owner = ownerId,
    startedAt = LocalDateTime.now(),
    duration = Duration.ofMinutes(30),
    volume = 10.some,
    comment = "text".some,
    deletedAt = None
  )

  private def makeUpdateTaskRequest(taskService: TaskService[IO]) = {
    val url  = s"/projects/$projectId/tasks/$taskId"
    val body = Stream.evalSeq(IO { taskInput.asJson.toString().getBytes.toSeq })
    makeRequest(
      request = Request(method = Method.PUT, uri = Uri.unsafeFromString(url), body = body),
      taskService = taskService
    )
  }

}
