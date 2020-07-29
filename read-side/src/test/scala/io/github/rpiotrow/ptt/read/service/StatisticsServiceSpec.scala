package io.github.rpiotrow.ptt.read.service

import java.time.Duration
import java.util.UUID

import com.softwaremill.diffx.scalatest.DiffMatcher._
import io.github.rpiotrow.projecttimetracker.api.output.StatisticsOutput
import io.github.rpiotrow.ptt.read.entity.StatisticsEntity
import io.github.rpiotrow.ptt.read.repository.{StatisticsRepository, YearMonth, YearMonthRange}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

import scala.math.BigDecimal.RoundingMode

class StatisticsServiceSpec extends AnyFunSpec with MockFactory with should.Matchers {

  val range = YearMonthRange(YearMonth(2020, 1), YearMonth(2020, 12))
  val ids   = List(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())

  describe("StatisticsService read() should") {
    it("return zero when repository returns empty list") {
      val statisticsRepository = mock[StatisticsRepository.Service]
      val service              = StatisticsService.live(statisticsRepository)

      (statisticsRepository.list _).expects(ids, range).returning(zio.IO.succeed(List()))
      val result =
        zio.Runtime.default.unsafeRun(service.read(ids, range))

      result should matchTo(StatisticsOutput.ZERO)
    }
    it("return one when repository returns one element list") {
      val statisticsRepository = mock[StatisticsRepository.Service]
      val service              = StatisticsService.live(statisticsRepository)

      val entity = StatisticsEntity(
        owner = ids.head,
        year = 2020,
        month = 1,
        numberOfTasks = 4,
        averageTaskDurationMinutes = 300,       // (120+120+480+480)/4
        averageTaskVolume = 2.5,                // (2+2+3+3)/4
        volumeWeightedTaskDurationSum = 3360.0, // 120*2 + 120*2 + 480*3 + 480*3
        volumeSum = 10.0                        // 2+2+3+3
      )
      val output = StatisticsOutput(
        numberOfTasks = 4,
        averageTaskDuration = Duration.ofMinutes(300),
        averageTaskVolume = 2.5,
        volumeWeightedAverageTaskDuration = Duration.ofMinutes((3360.0 / 10.0).toInt)
      )
      (statisticsRepository.list _).expects(ids, range).returning(zio.IO.succeed(List(entity)))
      val result =
        zio.Runtime.default.unsafeRun(service.read(ids, range))

      result should matchTo(output)
    }
    it("return sum when repository returns two-element list") {
      val statisticsRepository = mock[StatisticsRepository.Service]
      val service              = StatisticsService.live(statisticsRepository)

      val entity1 = StatisticsEntity(
        owner = ids.head,
        year = 2020,
        month = 1,
        numberOfTasks = 2,
        averageTaskDurationMinutes = 120,      // (60+180)/2
        averageTaskVolume = 1.5,               // (2+1)/2
        volumeWeightedTaskDurationSum = 300.0, // 60*2 + 180*1
        volumeSum = 3.0                        // 2+1
      )
      val entity2 = StatisticsEntity(
        owner = ids.head,
        year = 2020,
        month = 1,
        numberOfTasks = 3,
        averageTaskDurationMinutes = 240,       // (120+120+480)/3
        averageTaskVolume = 2.6667,             // (2+4+2)÷3
        volumeWeightedTaskDurationSum = 1680.0, // 120×2+120×4+480×2
        volumeSum = 8.0                         // 2+4+2
      )
      val output = StatisticsOutput(
        numberOfTasks = 5,
        averageTaskDuration = Duration.ofMinutes((2 * 120 + 3 * 240) / 5),
        averageTaskVolume = BigDecimal((2 * 1.5 + 3 * 2.6667) / 5.0).setScale(2, RoundingMode.HALF_UP),
        volumeWeightedAverageTaskDuration = Duration.ofMinutes(((300.0 + 1680.0) / (3.0 + 8.0)).toInt)
      )
      (statisticsRepository.list _).expects(ids, range).returning(zio.IO.succeed(List(entity1, entity2)))
      val result =
        zio.Runtime.default.unsafeRun(service.read(ids, range))

      result should matchTo(output)
    }
    it("return sum when repository returns three-element list") {
      val statisticsRepository = mock[StatisticsRepository.Service]
      val service              = StatisticsService.live(statisticsRepository)

      val entity1 = StatisticsEntity(
        owner = ids.head,
        year = 2020,
        month = 1,
        numberOfTasks = 2,
        averageTaskDurationMinutes = 120,      // (60+180)/2
        averageTaskVolume = 1.5,               // (2+1)/2
        volumeWeightedTaskDurationSum = 300.0, // 60*2 + 180*1
        volumeSum = 3.0                        // 2+1
      )
      val entity2 = StatisticsEntity(
        owner = ids.head,
        year = 2020,
        month = 1,
        numberOfTasks = 3,
        averageTaskDurationMinutes = 240,       // (120+120+480)/3
        averageTaskVolume = 2.6667,             // (2+4+2)÷3
        volumeWeightedTaskDurationSum = 1680.0, // 120×2+120×4+480×2
        volumeSum = 8.0                         // 2+4+2
      )
      val entity3 = StatisticsEntity(
        owner = ids(1),
        year = 2020,
        month = 3,
        numberOfTasks = 4,
        averageTaskDurationMinutes = 300,       // (120+120+480+480)/4
        averageTaskVolume = 2.5,                // (2+2+3+3)/4
        volumeWeightedTaskDurationSum = 3360.0, // 120*2 + 120*2 + 480*3 + 480*3
        volumeSum = 10.0                        // 2+2+3+3
      )
      val output = StatisticsOutput(
        numberOfTasks = 9,
        averageTaskDuration = Duration.ofMinutes((2 * 120 + 3 * 240 + 4 * 300) / 9),
        averageTaskVolume = BigDecimal((2 * 1.5 + 3 * 2.6667 + 4 * 2.5) / 9.0).setScale(2, RoundingMode.HALF_UP),
        volumeWeightedAverageTaskDuration = Duration.ofMinutes(((300.0 + 1680.0 + 3360.0) / (3.0 + 8.0 + 10.0)).toInt)
      )
      (statisticsRepository.list _).expects(ids, range).returning(zio.IO.succeed(List(entity1, entity2, entity3)))
      val result =
        zio.Runtime.default.unsafeRun(service.read(ids, range))

      result should matchTo(output)
    }
  }

}
