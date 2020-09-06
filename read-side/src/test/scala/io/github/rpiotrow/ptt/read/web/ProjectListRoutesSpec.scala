package io.github.rpiotrow.ptt.read.web

import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}

import cats.implicits._
import eu.timepit.refined.auto._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.refined._
import io.github.rpiotrow.ptt.api.error.ServerError
import io.github.rpiotrow.ptt.api.output.{ProjectOutput, TaskOutput}
import io.github.rpiotrow.ptt.api.param.OrderingDirection.{Ascending, Descending}
import io.github.rpiotrow.ptt.api.param.ProjectListParams
import io.github.rpiotrow.ptt.api.param.ProjectOrderingField.{CreatedAt, LastAddDurationAt}
import io.github.rpiotrow.ptt.read.repository.RepositoryThrowable
import io.github.rpiotrow.ptt.read.service.ProjectService
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.scalatest.funspec.AnyFunSpec
import zio.Runtime.default.unsafeRun
import zio.Task
import zio.interop.catz._

class ProjectListRoutesSpec extends AnyFunSpec with RoutesSpecBase {

  private val emptyParams = ProjectListParams(List(), None, None, None, None, None, 0, 25)

  describe("valid parameters") {
    it(s"$projects") {
      val url    = s"$projects"
      val params = emptyParams
      checkProjectList(url, params)
    }
    it(s"$projects?ids=project%20one&ids=project%20without%20tasks") {
      val url    = s"$projects?ids=project%20one&ids=project%20without%20tasks"
      val params = emptyParams.copy(ids = List("project one", "project without tasks"))
      checkProjectList(url, params)
    }
    it("check JSON") {
      val url    = s"$projects?ids=project%20one&ids=project%20without%20tasks"
      val params = emptyParams.copy(ids = List("project one", "project without tasks"))
      checkJson(url, params)
    }
    it(s"$projects?from=2020-02-03T10:11:50") {
      val url    = s"$projects?from=2020-02-03T10:11:50"
      val params = emptyParams.copy(from = LocalDateTime.of(2020, 2, 3, 10, 11, 50).some)
      checkProjectList(url, params)
    }
    it(s"$projects?to=2020-03-02T11:12:52") {
      val url    = s"$projects?to=2020-03-02T11:12:52"
      val params = emptyParams.copy(to = LocalDateTime.of(2020, 3, 2, 11, 12, 52).some)
      checkProjectList(url, params)
    }
    it(s"$projects?deleted=false") {
      val url    = s"$projects?deleted=false"
      val params = emptyParams.copy(deleted = Some(false))
      checkProjectList(url, params)
    }
    it(s"$projects?orderBy=CreatedAt") {
      val url    = s"$projects?orderBy=CreatedAt"
      val params = emptyParams.copy(orderBy = Some(CreatedAt))
      checkProjectList(url, params)
    }
    it(s"$projects?orderDirection=Descending") {
      val url    = s"$projects?orderDirection=Descending"
      val params = emptyParams.copy(orderDirection = Some(Descending))
      checkProjectList(url, params)
    }
    it(s"$projects?pageNumber=1") {
      val url    = s"$projects?pageNumber=1"
      val params = emptyParams.copy(pageNumber = 1)
      checkProjectList(url, params)
    }
    it(s"$projects?pageSize=10") {
      val url    = s"$projects?pageSize=10"
      val params = emptyParams.copy(pageSize = 10)
      checkProjectList(url, params)
    }
    it("all possible parameters") {
      val url    =
        s"""
           |$projects?ids=project%20one&ids=project%20without%20tasks
           |&from=2020-03-03T10:11:50
           |&to=2020-04-02T11:12:52
           |&deleted=true
           |&orderBy=LastAddDurationAt
           |&orderDirection=Ascending
           |&pageNumber=2
           |&pageSize=20
           |""".stripMargin.replaceAllLiterally("\n", "")
      val params = ProjectListParams(
        ids = List("project one", "project without tasks"),
        from = LocalDateTime.of(2020, 3, 3, 10, 11, 50).some,
        to = LocalDateTime.of(2020, 4, 2, 11, 12, 52).some,
        deleted = Some(true),
        orderBy = Some(LastAddDurationAt),
        orderDirection = Some(Ascending),
        pageNumber = 2,
        pageSize = 20
      )
      checkProjectList(url, params)
    }
  }
  describe("invalid parameters") {
    it(s"$projects?ids=") {
      checkBadRequest(s"$projects?ids=", "ids")
    }
    it(s"$projects?from=") {
      checkBadRequest(s"$projects?from=", "from")
    }
    it(s"$projects?from=2020/02/03") {
      checkBadRequest(s"$projects?from=2020/02/03", "from")
    }
    it(s"$projects?to=") {
      checkBadRequest(s"$projects?to=", "to")
    }
    it(s"$projects?to=2020/03/02") {
      checkBadRequest(s"$projects?to=2020/03/02", "to")
    }
    it(s"$projects?deleted=") {
      checkBadRequest(s"$projects?deleted=", "deleted")
    }
    it(s"$projects?deleted=not") {
      checkBadRequest(s"$projects?deleted=not", "deleted")
    }
    it(s"$projects?deleted=0") {
      checkBadRequest(s"$projects?deleted=0", "deleted")
    }
    it(s"$projects?orderBy=") {
      checkBadRequest(s"$projects?orderBy=", "orderBy")
    }
    it(s"$projects?orderBy=NotCreatedAt") {
      checkBadRequest(s"$projects?orderBy=NotCreatedAt", "orderBy")
    }
    it(s"$projects?orderBy=0") {
      checkBadRequest(s"$projects?orderBy=0", "orderBy")
    }
    it(s"$projects?orderDirection=") {
      checkBadRequest(s"$projects?orderDirection=", "orderDirection")
    }
    it(s"$projects?orderDirection=DESC") {
      checkBadRequest(s"$projects?orderDirection=DESC", "orderDirection")
    }
    it(s"$projects?orderDirection=-1") {
      checkBadRequest(s"$projects?orderDirection=-1", "orderDirection")
    }
    it(s"$projects?pageNumber=") {
      checkBadRequest(s"$projects?pageNumber=", "pageNumber")
    }
    it(s"$projects?pageNumber=abc") {
      checkBadRequest(s"$projects?pageNumber=abc", "pageNumber")
    }
    it(s"$projects?pageNumber=-1") {
      checkBadRequest(s"$projects?pageNumber=-1", "pageNumber")
    }
    it(s"$projects?pageSize=") {
      checkBadRequest(s"$projects?pageSize=", "pageSize")
    }
    it(s"$projects?pageSize=x") {
      checkBadRequest(s"$projects?pageSize=x", "pageSize")
    }
    it(s"$projects?pageSize=0") {
      checkBadRequest(s"$projects?pageSize=0", "pageSize")
    }
  }
  describe("service error") {
    it(s"$projects") {
      val url            = s"$projects"
      val projectService = mock[ProjectService.Service]
      (projectService.list _)
        .expects(emptyParams)
        .returning(zio.IO.fail(RepositoryThrowable(new RuntimeException("network error"))))

      val response = makeRequest(Request(uri = Uri.unsafeFromString(url)), projectService)
      val body     = unsafeRun(response.as[ServerError])

      response.status should be(Status.InternalServerError)
      body should be(ServerError("server.error"))
    }
  }

  private def projectList(params: ProjectListParams) = {
    val projectService = mock[ProjectService.Service]
    (projectService.list _).expects(params).returning(zio.IO.succeed(List(projectOutput1, projectOutput2)))
    projectService
  }

  private def checkProjectList(url: String, params: ProjectListParams) = {
    val response = makeRequest(Request(uri = Uri.unsafeFromString(url)), projectList(params))

    response.status should be(Status.Ok)
    bodyAsProjectOutputList(response) should be(List(projectOutput1, projectOutput2))
  }

  private def checkJson(url: String, params: ProjectListParams) = {
    def taskToJsonObject(task: TaskOutput) = {
      Json.obj(
        "taskId"    -> Json.fromString(task.taskId.id.toString),
        "owner"     -> Json.fromString(task.owner.id.toString),
        "startedAt" -> Json.fromString(dateTimeString(task.startedAt)),
        "duration"  -> Json.fromString(task.duration.toString),
        "volume"    -> task.volume.map(Json.fromInt).getOrElse(Json.Null),
        "comment"   -> task.comment.map(Json.fromString).getOrElse(Json.Null),
        "deletedAt" -> task.deletedAt.map(e => Json.fromString(dateTimeString(e))).getOrElse(Json.Null)
      )
    }

    val response = makeRequest(Request(uri = Uri.unsafeFromString(url)), projectList(params))

    bodyAsJson(response) should be(
      Right(
        Json.fromValues(
          List(
            Json.obj(
              "projectId"   -> Json.fromString(projectOutput1.projectId.toString),
              "createdAt"   -> Json.fromString(dateTimeString(projectOutput1.createdAt)),
              "deletedAt"   -> Json.Null,
              "owner"       -> Json.fromString(projectOutput1.owner.id.toString),
              "durationSum" -> Json.fromString(projectOutput1.durationSum.toString),
              "tasks"       -> Json
                .fromValues(List(taskToJsonObject(projectOutput1.tasks(0)), taskToJsonObject(projectOutput1.tasks(1))))
            ),
            Json.obj(
              "projectId"   -> Json.fromString(projectOutput2.projectId.toString),
              "createdAt"   -> Json.fromString(dateTimeString(projectOutput2.createdAt)),
              "deletedAt"   -> Json.Null,
              "owner"       -> Json.fromString(projectOutput2.owner.id.toString),
              "durationSum" -> Json.fromString(projectOutput2.durationSum.toString),
              "tasks"       -> Json.fromValues(List())
            )
          )
        )
      )
    )
  }

  private def dateTimeString(localDateTime: LocalDateTime): String = localDateTime.format(ISO_LOCAL_DATE_TIME)

  private def bodyAsProjectOutputList(request: Response[Task]) = {
    import io.github.rpiotrow.ptt.api.CirceMappings._
    unsafeRun(request.as[List[ProjectOutput]])
  }

}
