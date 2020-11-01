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
import io.github.rpiotrow.ptt.api.model._
import io.github.rpiotrow.ptt.api.output.TaskOutput
import io.github.rpiotrow.ptt.write.service._
import org.http4s.headers.Location
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Method, Request, Status, Uri}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should
import sttp.model.HeaderNames

class TaskAddRouteSpec extends AnyFunSpec with RouteSpecBase with MockFactory with should.Matchers {

  private val projectId: ProjectId = "pp1"
  private val taskIdUUID           = UUID.randomUUID()
  private val taskId: TaskId       = TaskId(taskIdUUID)

  describe(s"POST /projects/$projectId/tasks") {
    it("successful") {
      val taskService = mock[TaskService[IO]]
      (taskService.add _).expects(projectId, taskInput, ownerId).returning(EitherT.right(IO(taskId)))
      val response    = makeAddTaskRequest(taskService)

      response.status should be(Status.Created)
      response.headers.find(_.name == CaseInsensitiveString(HeaderNames.Location)) should be(
        Some(Location(Uri.unsafeFromString(s"http://gateway.live/projects/pp1/tasks/$taskIdUUID")))
      )
    }
    describe("failure") {
      it("when project does not exist") {
        val taskService = mock[TaskService[IO]]
        (taskService.add _)
          .expects(projectId, taskInput, ownerId)
          .returning(EitherT.left(IO(EntityNotFound("project with given projectId does not exist"))))
        val response    = makeAddTaskRequest(taskService)

        response.status should be(Status.NotFound)
        response.headers.find(_.name == CaseInsensitiveString(HeaderNames.Location)) should be(None)
      }
      it("when task time span is invalid") {
        val taskService            = mock[TaskService[IO]]
        val appFailure: AppFailure = InvalidTimeSpan
        (taskService.add _)
          .expects(projectId, taskInput, ownerId)
          .returning(EitherT.left(IO(appFailure)))
        val response               = makeAddTaskRequest(taskService)

        response.status should be(Status.Conflict)
        response.headers.find(_.name == CaseInsensitiveString(HeaderNames.Location)) should be(None)
      }
      it("when project is deleted") {
        val taskService = mock[TaskService[IO]]
        (taskService.add _)
          .expects(projectId, taskInput, ownerId)
          .returning(EitherT.left(IO(AlreadyDeleted("project was deleted"))))
        val response    = makeAddTaskRequest(taskService)

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
    taskId = taskId,
    owner = ownerId,
    startedAt = OffsetDateTime.now(),
    duration = Duration.ofMinutes(30),
    volume = 10.some,
    comment = "text".some,
    deletedAt = None
  )

  private def makeAddTaskRequest(taskService: TaskService[IO]) = {
    val url  = s"/projects/$projectId/tasks"
    val body = Stream.evalSeq(IO { taskInput.asJson.toString().getBytes.toSeq })
    makeRequest(
      request = Request(method = Method.POST, uri = Uri.unsafeFromString(url), body = body),
      taskService = taskService
    )
  }

}
