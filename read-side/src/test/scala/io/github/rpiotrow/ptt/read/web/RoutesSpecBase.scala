package io.github.rpiotrow.ptt.read.web

import java.time.{Duration, OffsetDateTime}
import java.util.UUID
import eu.timepit.refined.auto._
import cats.implicits._
import io.github.rpiotrow.ptt.api.error.DecodeFailure
import io.github.rpiotrow.ptt.api.output.{ProjectOutput, StatisticsOutput, TaskOutput}
import io.github.rpiotrow.ptt.read.service.{ProjectService, StatisticsService}
import org.http4s._
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should
import zio.Runtime.default.unsafeRun
import zio._
import io.circe.generic.auto._
import io.github.rpiotrow.ptt.api.model.{TaskId, UserId}
import zio.interop.catz._
import zio.interop.catz.implicits._
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.scalatest.Assertion

trait RoutesSpecBase extends MockFactory with should.Matchers {

  type RequestIO  = Request[RIO[WebEnv, *]]
  type ResponseIO = Response[RIO[WebEnv, *]]

  val projects   = "/projects"
  val statistics = "/statistics"

  val owner1Id: UserId                   = UserId(UUID.randomUUID())
  val projectOutput1: ProjectOutput      = ProjectOutput(
    projectId = "project one",
    createdAt = OffsetDateTime.now(),
    deletedAt = None,
    owner = owner1Id,
    durationSum = Duration.ofHours(3),
    tasks = List(
      TaskOutput(
        taskId = TaskId.random(),
        owner = owner1Id,
        startedAt = OffsetDateTime.now(),
        duration = Duration.ofHours(2),
        volume = None,
        comment = Some("first task"),
        deletedAt = None
      ),
      TaskOutput(
        taskId = TaskId.random(),
        owner = owner1Id,
        startedAt = OffsetDateTime.now(),
        duration = Duration.ofHours(1),
        volume = Some(4),
        comment = Some("second task"),
        deletedAt = None
      )
    )
  )
  val projectOutput2                     = ProjectOutput(
    projectId = "project without tasks",
    owner = owner1Id,
    createdAt = OffsetDateTime.now(),
    deletedAt = None,
    durationSum = Duration.ZERO,
    tasks = List()
  )
  val statisticsOutput: StatisticsOutput = StatisticsOutput(
    numberOfTasks = 5,
    averageTaskDuration = Duration.ofMinutes(32).some,
    averageTaskVolume = BigDecimal(10.5).some,
    volumeWeightedAverageTaskDuration = Duration.ofMinutes(44).some
  )

  def bodyAsJson(response: ResponseIO) = {
    import io.circe.parser._
    parse(unsafeRun(response.bodyText.compile.string))
  }

  def makeRequest(
    request: RequestIO,
    projectService: ProjectService.Service = mock[ProjectService.Service],
    statisticsService: StatisticsService.Service = mock[StatisticsService.Service]
  ): ResponseIO = {
    val app: ZIO[WebEnv with Routes, Throwable, ResponseIO] = for {
      routes   <- Routes.readSideRoutes()
      response <- routes.run(request).value
    } yield response.getOrElse(Response.notFound)

    val services =
      ZLayer.fromFunction[Any, ProjectService.Service](_ => projectService) ++
        ZLayer.fromFunction[Any, StatisticsService.Service](_ => statisticsService)
    val webEnv   = zio.blocking.Blocking.live ++ zio.clock.Clock.live
    unsafeRun(app.provideLayer(webEnv ++ (services >>> Routes.live)))
  }

  def checkBadRequest(url: String, parameterName: String): Assertion = {
    val response = makeRequest(Request(uri = Uri.unsafeFromString(url)))

    response.status should be(Status.BadRequest)
    decodeFailure(response).message should startWith(s"Invalid value for: query parameter $parameterName")
  }

  def decodeFailure(response: ResponseIO): DecodeFailure = {
    unsafeRun(response.as[DecodeFailure])
  }

}
