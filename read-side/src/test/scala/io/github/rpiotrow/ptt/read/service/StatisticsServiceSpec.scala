package io.github.rpiotrow.ptt.read.service

import java.time.{Duration, YearMonth}
import java.util.UUID

import com.softwaremill.diffx.generic.auto._
import com.softwaremill.diffx.scalatest.DiffShouldMatcher._
import io.github.rpiotrow.ptt.api.output.StatisticsOutput
import io.github.rpiotrow.ptt.api.param.StatisticsParams
import io.github.rpiotrow.ptt.read.entity.StatisticsEntity
import io.github.rpiotrow.ptt.read.repository.StatisticsRepository
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should
import cats.implicits._
import io.github.rpiotrow.ptt.api.model.{NonEmptyUserIdList, UserId}

import scala.math.BigDecimal.RoundingMode

class StatisticsServiceSpec extends AnyFunSpec with MockFactory with should.Matchers {

  val params = StatisticsParams(
    NonEmptyUserIdList.of(UserId(UUID.randomUUID()), UserId(UUID.randomUUID()), UserId(UUID.randomUUID())),
    YearMonth.of(2020, 1),
    YearMonth.of(2020, 12)
  )

  describe("StatisticsService read() should") {
    it("return zero when repository returns empty list") {
      val statisticsRepository = mock[StatisticsRepository.Service]
      val service              = StatisticsService.live(statisticsRepository)

      (statisticsRepository.list _).expects(params).returning(zio.IO.succeed(List()))
      val result =
        zio.Runtime.default.unsafeRun(service.read(params))

      result shouldMatchTo(StatisticsOutput.ZERO)
    }
    it("return one when repository returns one element list") {
      val statisticsRepository = mock[StatisticsRepository.Service]
      val service              = StatisticsService.live(statisticsRepository)

      val entity = StatisticsEntity(
        owner = params.ids.list.head,
        year = 2020,
        month = 1,
        numberOfTasks = 4,
        numberOfTasksWithVolume = 4.some,
        durationSum = Duration.ofMinutes(1200),                       // 120+120+480+480
        volumeSum = 10L.some,                                         // 2+2+3+3
        volumeWeightedTaskDurationSum = Duration.ofMinutes(3360).some // 120*2 + 120*2 + 480*3 + 480*3
      )
      val output = StatisticsOutput(
        numberOfTasks = 4,
        averageTaskDuration = Duration.ofMinutes(300).some,
        averageTaskVolume = BigDecimal(2.5).some,
        volumeWeightedAverageTaskDuration = Duration.ofMinutes((3360.0 / 10.0).toInt).some
      )
      (statisticsRepository.list _).expects(params).returning(zio.IO.succeed(List(entity)))
      val result =
        zio.Runtime.default.unsafeRun(service.read(params))

      result shouldMatchTo(output)
    }
    it("return sum when repository returns two-element list") {
      val statisticsRepository = mock[StatisticsRepository.Service]
      val service              = StatisticsService.live(statisticsRepository)

      val entity1 = StatisticsEntity(
        owner = params.ids.list.head,
        year = 2020,
        month = 1,
        numberOfTasks = 2,
        numberOfTasksWithVolume = 2.some,
        durationSum = Duration.ofMinutes(240),                       // 60+180
        volumeSum = 3L.some,                                         // 2+1
        volumeWeightedTaskDurationSum = Duration.ofMinutes(300).some // 60*2 + 180*1
      )
      val entity2 = StatisticsEntity(
        owner = params.ids.list.head,
        year = 2020,
        month = 1,
        numberOfTasks = 3,
        numberOfTasksWithVolume = 3.some,
        durationSum = Duration.ofMinutes(720),                        // 120+120+480
        volumeSum = 8L.some,                                          // 2+4+2
        volumeWeightedTaskDurationSum = Duration.ofMinutes(1680).some // 120×2+120×4+480×2
      )
      val output = StatisticsOutput(
        numberOfTasks = 5,
        averageTaskDuration = Duration.ofMinutes((240 + 720) / 5).some,
        averageTaskVolume = BigDecimal((3 + 8) / 5.0).setScale(2, RoundingMode.HALF_UP).some,
        volumeWeightedAverageTaskDuration = Duration.ofMinutes(((300.0 + 1680.0) / (3.0 + 8.0)).toInt).some
      )
      (statisticsRepository.list _).expects(params).returning(zio.IO.succeed(List(entity1, entity2)))
      val result =
        zio.Runtime.default.unsafeRun(service.read(params))

      result shouldMatchTo(output)
    }
    it("return sum when repository returns three-element list") {
      val statisticsRepository = mock[StatisticsRepository.Service]
      val service              = StatisticsService.live(statisticsRepository)

      val entity1 = StatisticsEntity(
        owner = params.ids.list.head,
        year = 2020,
        month = 1,
        numberOfTasks = 2,
        numberOfTasksWithVolume = 2.some,
        durationSum = Duration.ofMinutes(240),                       // 60+180
        volumeSum = 3L.some,                                         // 2+1
        volumeWeightedTaskDurationSum = Duration.ofMinutes(300).some // 60*2 + 180*1
      )
      val entity2 = StatisticsEntity(
        owner = params.ids.list.head,
        year = 2020,
        month = 1,
        numberOfTasks = 3,
        numberOfTasksWithVolume = 3.some,
        durationSum = Duration.ofMinutes(720),                        // 120+120+480
        volumeSum = 8L.some,                                          // 2+4+2
        volumeWeightedTaskDurationSum = Duration.ofMinutes(1680).some // 120×2+120×4+480×2
      )
      val entity3 = StatisticsEntity(
        owner = params.ids.toList(1),
        year = 2020,
        month = 3,
        numberOfTasks = 4,
        numberOfTasksWithVolume = 4.some,
        durationSum = Duration.ofMinutes(1200),                       // 120+120+480+480
        volumeSum = 10L.some,                                         // 2+2+3+3
        volumeWeightedTaskDurationSum = Duration.ofMinutes(3360).some // 120*2 + 120*2 + 480*3 + 480*3
      )
      val output = StatisticsOutput(
        numberOfTasks = 9,
        averageTaskDuration = Duration.ofMinutes((240 + 720 + 1200) / 9).some,
        averageTaskVolume = BigDecimal((3 + 8 + 10) / 9.0).setScale(2, RoundingMode.HALF_UP).some,
        volumeWeightedAverageTaskDuration =
          Duration.ofSeconds((((300.0 + 1680.0 + 3360.0) / (3.0 + 8.0 + 10.0)) * 60).intValue).some
      )
      (statisticsRepository.list _).expects(params).returning(zio.IO.succeed(List(entity1, entity2, entity3)))
      val result =
        zio.Runtime.default.unsafeRun(service.read(params))

      result shouldMatchTo(output)
    }
    it("return sum when repository returns three-element list with task without volume") {
      val statisticsRepository = mock[StatisticsRepository.Service]
      val service              = StatisticsService.live(statisticsRepository)

      val entity1 = StatisticsEntity(
        owner = params.ids.list.head,
        year = 2020,
        month = 1,
        numberOfTasks = 2,
        numberOfTasksWithVolume = None,
        durationSum = Duration.ofMinutes(240), // 60+180
        volumeSum = None,
        volumeWeightedTaskDurationSum = None
      )
      val entity2 = StatisticsEntity(
        owner = params.ids.list.head,
        year = 2020,
        month = 1,
        numberOfTasks = 3,
        numberOfTasksWithVolume = None,
        durationSum = Duration.ofMinutes(720), // 120+120+480
        volumeSum = None,
        volumeWeightedTaskDurationSum = None
      )
      val entity3 = StatisticsEntity(
        owner = params.ids.toList(1),
        year = 2020,
        month = 3,
        numberOfTasks = 4,
        numberOfTasksWithVolume = 2.some,
        durationSum = Duration.ofMinutes(1200),                      // 120+120+480+480
        volumeSum = 10L.some,                                        // 8+2
        volumeWeightedTaskDurationSum = Duration.ofMinutes(480).some // 120*2 + 120*2
      )
      val output = StatisticsOutput(
        numberOfTasks = 9,
        averageTaskDuration = Duration.ofMinutes((240 + 720 + 1200) / 9).some,
        averageTaskVolume = BigDecimal(10 / 2.0).setScale(2, RoundingMode.HALF_UP).some,
        volumeWeightedAverageTaskDuration = Duration.ofSeconds(((480.0 / 10.0) * 60).intValue).some
      )
      (statisticsRepository.list _).expects(params).returning(zio.IO.succeed(List(entity1, entity2, entity3)))
      val result =
        zio.Runtime.default.unsafeRun(service.read(params))

      result shouldMatchTo(output)
    }
  }

}
