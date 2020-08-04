package io.github.rpiotrow.ptt.read.service

import java.time.Duration
import java.util.UUID

import cats.Monoid
import cats.implicits._
import io.github.rpiotrow.projecttimetracker.api.output.StatisticsOutput
import io.github.rpiotrow.projecttimetracker.api.param.StatisticsParams
import io.github.rpiotrow.ptt.read.entity.StatisticsEntity
import io.github.rpiotrow.ptt.read.repository._
import zio.{IO, Task}

import scala.math.BigDecimal.RoundingMode

object StatisticsService {
  trait Service {
    def read(params: StatisticsParams): IO[RepositoryThrowable, StatisticsOutput]
  }
  def live(statisticsRepository: StatisticsRepository.Service): Service =
    new StatisticsServiceLive(statisticsRepository)
}

private case class Statistics(
  numberOfTasks: Int,
  averageTaskDurationMinutes: Int,
  averageTaskVolume: BigDecimal,
  volumeWeightedTaskDurationSum: BigDecimal,
  volumeSum: BigDecimal
)

private object Statistics {
  implicit val statisticsMonoid: Monoid[Statistics] = new Monoid[Statistics] {
    override def empty: Statistics = Statistics(0, 0, 0.0, 0.0, 0.0)

    override def combine(x: Statistics, y: Statistics): Statistics = {
      val numberOfTasksSum = x.numberOfTasks + y.numberOfTasks
      val taskDurationSum  =
        x.numberOfTasks * x.averageTaskDurationMinutes + y.numberOfTasks * y.averageTaskDurationMinutes
      val taskVolumeSum    = x.numberOfTasks * x.averageTaskVolume + y.numberOfTasks * y.averageTaskVolume
      Statistics(
        numberOfTasks = numberOfTasksSum,
        averageTaskDurationMinutes = taskDurationSum / numberOfTasksSum,
        averageTaskVolume = taskVolumeSum / numberOfTasksSum,
        volumeWeightedTaskDurationSum = x.volumeWeightedTaskDurationSum + y.volumeWeightedTaskDurationSum,
        volumeSum = x.volumeSum + y.volumeSum
      )
    }
  }
}

private class StatisticsServiceLive(private val statisticsRepository: StatisticsRepository.Service)
    extends StatisticsService.Service {

  override def read(params: StatisticsParams) = {
    for {
      entities <- statisticsRepository.list(params)
      statistics = entities.map(toStatistics).combineAll
    } yield toOutput(statistics)
  }

  private def toStatistics(entity: StatisticsEntity) =
    Statistics(
      numberOfTasks = entity.numberOfTasks,
      averageTaskDurationMinutes = entity.averageTaskDurationMinutes,
      averageTaskVolume = entity.averageTaskVolume,
      volumeWeightedTaskDurationSum = entity.volumeWeightedTaskDurationSum,
      volumeSum = entity.volumeSum
    )

  private def toOutput(statistics: Statistics) =
    StatisticsOutput(
      numberOfTasks = statistics.numberOfTasks,
      averageTaskDuration = Duration.ofMinutes(statistics.averageTaskDurationMinutes),
      averageTaskVolume = statistics.averageTaskVolume.setScale(2, RoundingMode.HALF_UP),
      volumeWeightedAverageTaskDuration =
        if (statistics.volumeSum > 0)
          Duration.ofMinutes((statistics.volumeWeightedTaskDurationSum / statistics.volumeSum).toInt)
        else
          Duration.ZERO
    )
}
