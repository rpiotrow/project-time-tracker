package io.github.rpiotrow.ptt.read.web

import java.time.{Duration, LocalDateTime}
import java.util.UUID

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
import zio.interop.catz._
import zio.interop.catz.implicits._
import org.http4s._
import org.http4s.circe.CirceEntityCodec._

trait RoutesSpecBase extends MockFactory with should.Matchers {

  val projects   = "/projects"
  val statistics = "/statistics"

  val owner1Id                           = UUID.randomUUID()
  val projectOutput1                     = ProjectOutput(
    projectId = "project one",
    createdAt = LocalDateTime.now(),
    deletedAt = None,
    owner = owner1Id,
    durationSum = Duration.ofHours(3),
    tasks = List(
      TaskOutput(
        taskId = UUID.randomUUID(),
        owner = owner1Id,
        startedAt = LocalDateTime.now(),
        duration = Duration.ofHours(2),
        volume = None,
        comment = Some("first task"),
        deletedAt = None
      ),
      TaskOutput(
        taskId = UUID.randomUUID(),
        owner = owner1Id,
        startedAt = LocalDateTime.now(),
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
    createdAt = LocalDateTime.now(),
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

  def makeRequest(
    request: Request[Task],
    projectService: ProjectService.Service = mock[ProjectService.Service],
    statisticsService: StatisticsService.Service = mock[StatisticsService.Service]
  ): Response[Task] = {
    val app = for {
      routes   <- Routes.readSideRoutes()
      response <- routes.run(request).value
    } yield response.getOrElse(Response.notFound)

    val services =
      ZLayer.fromFunction[Any, ProjectService.Service](_ => projectService) ++
        ZLayer.fromFunction[Any, StatisticsService.Service](_ => statisticsService)
    unsafeRun(app.provideLayer(services >>> Routes.live))
  }

  def checkBadRequest(url: String, parameterName: String) = {
    val response = makeRequest(Request(uri = Uri.unsafeFromString(url)))

    response.status should be(Status.BadRequest)
    decodeFailure(response).message should startWith(s"Invalid value for: query parameter $parameterName")
  }

  def decodeFailure(response: Response[Task]): DecodeFailure = {
    unsafeRun(response.as[DecodeFailure])
  }

}
