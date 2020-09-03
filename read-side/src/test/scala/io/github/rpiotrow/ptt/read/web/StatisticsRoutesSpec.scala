package io.github.rpiotrow.ptt.read.web

import java.time.YearMonth
import java.util.UUID

import cats.data.NonEmptyList
import io.circe.generic.auto._
import io.github.rpiotrow.ptt.api.error.ServerError
import io.github.rpiotrow.ptt.api.model.{NonEmptyUserIdList, UserId}
import io.github.rpiotrow.ptt.api.output.StatisticsOutput
import io.github.rpiotrow.ptt.api.param.StatisticsParams
import io.github.rpiotrow.ptt.read.repository.RepositoryThrowable
import io.github.rpiotrow.ptt.read.service.StatisticsService
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.scalatest.funspec.AnyFunSpec
import zio.Runtime.default.unsafeRun
import zio._
import zio.interop.catz._

class StatisticsRoutesSpec extends AnyFunSpec with RoutesSpecBase {

  import io.github.rpiotrow.ptt.api.CirceMappings._

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

  private val user1IdUUID = "1ce7e1e2-8bd6-4e2a-936f-342bc2ad5b1d"
  private val user1Id     = UserId(user1IdUUID)
  private val user2IdUUID = "b53ebacb-b6ce-4454-bfee-3b139bcc0d96"
  private val user2Id     = UserId(user2IdUUID)

  describe("valid parameters") {
    it(s"$statistics?ids=$user1IdUUID&from=2020-01&to=2020-12") {
      val url    = s"$statistics?ids=$user1IdUUID&from=2020-01&to=2020-12"
      val params = StatisticsParams(NonEmptyUserIdList.of(user1Id), YearMonth.of(2020, 1), YearMonth.of(2020, 12))
      checkStatistics(url, params)
    }
    it(s"$statistics?ids=$user1IdUUID&from=2020-01&to=2020-01") {
      val url    = s"$statistics?ids=$user1IdUUID&from=2020-01&to=2020-01"
      val params = StatisticsParams(NonEmptyUserIdList.of(user1Id), YearMonth.of(2020, 1), YearMonth.of(2020, 1))
      checkStatistics(url, params)
    }
    it(s"$statistics?ids=$user1IdUUID&ids=$user2IdUUID&from=2020-01&to=2020-12") {
      val url    = s"$statistics?ids=$user1IdUUID&ids=$user2IdUUID&from=2020-01&to=2020-12"
      val params =
        StatisticsParams(NonEmptyUserIdList.of(user1Id, user2Id), YearMonth.of(2020, 1), YearMonth.of(2020, 12))
      checkStatistics(url, params)
    }
  }
  describe("invalid parameters") {
    describe("from") {
      it(s"$statistics?ids=$user1IdUUID&to=2020-12") {
        checkBadRequest(s"$statistics?ids=$user1IdUUID&to=2020-12", "from")
      }
      it(s"$statistics?ids=$user1IdUUID&from=&to=2020-12") {
        checkBadRequest(s"$statistics?ids=$user1IdUUID&from=&to=2020-12", "from")
      }
      it(s"$statistics?ids=$user1IdUUID&from=123456&to=2020-12") {
        checkBadRequest(s"$statistics?ids=$user1IdUUID&from=123456&to=2020-12", "from")
      }
    }
    describe("to") {
      it(s"$statistics?ids=$user1IdUUID&from=2020-12") {
        checkBadRequest(s"$statistics?ids=$user1IdUUID&from=2020-12", "to")
      }
      it(s"$statistics?ids=$user1IdUUID&from=2020-12&to=") {
        checkBadRequest(s"$statistics?ids=$user1IdUUID&from=2020-12&to=", "to")
      }
      it(s"$statistics?ids=$user1IdUUID&from=2020-12&to=2020") {
        checkBadRequest(s"$statistics?ids=$user1IdUUID&from=2020-12&to=2020", "to")
      }
    }
    describe("from-to") {
      it(s"$statistics?ids=$user1IdUUID&from=2020-02&to=2020-01") {
        val url      = s"$statistics?ids=$user1IdUUID&from=2020-02&to=2020-01"
        val response = makeRequest(Request(uri = Uri.unsafeFromString(url)))

        response.status should be(Status.BadRequest)
        decodeFailure(response).message should startWith(
          "Invalid value (expected value to pass custom validation: `from` before or equal `to`"
        )
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
      it(s"$statistics?ids=$user1IdUUID&ids=not-uuid&from=2020-01&to=2020-12") {
        checkBadRequest(s"$statistics?ids=$user1IdUUID&ids=not-uuid&from=2020-01&to=2020-12", "ids")
      }
    }
  }
  it("service error") {
    val url         = s"$statistics?ids=$user1IdUUID&from=2020-01&to=2020-12"
    val params      = StatisticsParams(NonEmptyUserIdList.of(user1Id), YearMonth.of(2020, 1), YearMonth.of(2020, 12))
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
