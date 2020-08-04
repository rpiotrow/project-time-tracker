package io.github.rpiotrow.ptt.read.web

import java.time.YearMonth
import java.util.UUID

import cats.data.NonEmptyList
import io.circe.generic.auto._
import io.github.rpiotrow.projecttimetracker.api.error.ServerError
import io.github.rpiotrow.projecttimetracker.api.output.StatisticsOutput
import io.github.rpiotrow.projecttimetracker.api.param.StatisticsParams
import io.github.rpiotrow.ptt.read.repository.RepositoryThrowable
import io.github.rpiotrow.ptt.read.service.StatisticsService
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.scalatest.funspec.AnyFunSpec
import zio.Runtime.default.unsafeRun
import zio._
import zio.interop.catz._

class StatisticsRoutesSpec extends AnyFunSpec with RoutesSpecBase {

  private def statisticsService(params: StatisticsParams) = {
    val mockService = mock[StatisticsService.Service]
    (mockService.read _).expects(params).returning(zio.IO.succeed(statisticsOutput))
    mockService
  }

  private def checkStatistics(url: String, params: StatisticsParams) = {
    val request = makeRequest(Request(uri = Uri.unsafeFromString(url)), statisticsService = statisticsService(params))

    request.status should be(Status.Ok)
    body(request) should be(statisticsOutput)
  }

  private def body(response: Response[Task]): StatisticsOutput = {
    unsafeRun(response.as[StatisticsOutput])
  }

  private val user1Id = UUID.randomUUID()
  private val user2Id = UUID.randomUUID()

  describe("valid parameters") {
    it(s"$statistics?ids=$user1Id&from=2020-01&to=2020-12") {
      val url    = s"$statistics?ids=$user1Id&from=2020-01&to=2020-12"
      val params = StatisticsParams(NonEmptyList.of(user1Id), YearMonth.of(2020, 1), YearMonth.of(2020, 12))
      checkStatistics(url, params)
    }
    it(s"$statistics?ids=$user1Id&ids=$user2Id&from=2020-01&to=2020-12") {
      val url    = s"$statistics?ids=$user1Id&ids=$user2Id&from=2020-01&to=2020-12"
      val params = StatisticsParams(NonEmptyList.of(user1Id, user2Id), YearMonth.of(2020, 1), YearMonth.of(2020, 12))
      checkStatistics(url, params)
    }
  }
  describe("invalid parameters") {
    describe("from") {
      it(s"$statistics?ids=$user1Id&to=2020-12") {
        checkBadRequest(s"$statistics?ids=$user1Id&to=2020-12", "from")
      }
      it(s"$statistics?ids=$user1Id&from=&to=2020-12") {
        checkBadRequest(s"$statistics?ids=$user1Id&from=&to=2020-12", "from")
      }
      it(s"$statistics?ids=$user1Id&from=123456&to=2020-12") {
        checkBadRequest(s"$statistics?ids=$user1Id&from=123456&to=2020-12", "from")
      }
    }
    describe("to") {
      it(s"$statistics?ids=$user1Id&from=2020-12") {
        checkBadRequest(s"$statistics?ids=$user1Id&from=2020-12", "to")
      }
      it(s"$statistics?ids=$user1Id&from=2020-12&to=") {
        checkBadRequest(s"$statistics?ids=$user1Id&from=2020-12&to=", "to")
      }
      it(s"$statistics?ids=$user1Id&from=2020-12&to=2020") {
        checkBadRequest(s"$statistics?ids=$user1Id&from=2020-12&to=2020", "to")
      }
    }
    describe("ids") {
      it(s"$statistics?from=2020-01&to=2020-12") {
        checkBadRequest(s"$statistics?from=2020-01&to=2020-12", "ids")
      }
      it(s"$statistics?ids=&from=2020-01&to=2020-12") {
        checkBadRequest(s"$statistics?ids=&from=2020-01&to=2020-12", "ids")
      }
      it(s"$statistics?ids=not-uuid&from=2020-01&to=2020-12") {
        checkBadRequest(s"$statistics?ids=not-uuid&from=2020-01&to=2020-12", "ids")
      }
      it(s"$statistics?ids=$user1Id&ids=not-uuid&from=2020-01&to=2020-12") {
        checkBadRequest(s"$statistics?ids=$user1Id&ids=not-uuid&from=2020-01&to=2020-12", "ids")
      }
    }
  }
  it("service error") {
    val url         = s"$statistics?ids=$user1Id&from=2020-01&to=2020-12"
    val params      = StatisticsParams(NonEmptyList.of(user1Id), YearMonth.of(2020, 1), YearMonth.of(2020, 12))
    val mockService = mock[StatisticsService.Service]
    (mockService.read _)
      .expects(params)
      .returning(IO.fail(RepositoryThrowable(new RuntimeException("no connection to database"))))

    val response = makeRequest(Request(uri = Uri.unsafeFromString(url)), statisticsService = mockService)
    val body     = unsafeRun(response.as[ServerError])

    response.status should be(Status.InternalServerError)
    body should be(ServerError("server.error"))
  }

}
