package io.github.rpiotrow.ptt.read.service

import java.time.Duration
import java.time.temporal.ChronoUnit

import cats.Monoid
import cats.implicits._
import io.github.rpiotrow.ptt.api.output.StatisticsOutput
import io.github.rpiotrow.ptt.api.param.StatisticsParams
import io.github.rpiotrow.ptt.read.entity.StatisticsEntity
import io.github.rpiotrow.ptt.read.repository._
import zio.IO

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
  numberOfTasksWithVolume: Option[Int],
  durationSum: Duration,
  volumeSum: Option[Long],
  volumeWeightedTaskDurationSum: Option[Duration]
)

private object Statistics {
  implicit val statisticsMonoid: Monoid[Statistics] = new Monoid[Statistics] {
    override def empty: Statistics = Statistics(0, None, Duration.ZERO, None, None)

    override def combine(x: Statistics, y: Statistics): Statistics = {
      Statistics(
        numberOfTasks = x.numberOfTasks + y.numberOfTasks,
        numberOfTasksWithVolume = (x.numberOfTasksWithVolume ++ y.numberOfTasksWithVolume).reduceOption(_ + _),
        durationSum = x.durationSum.plus(y.durationSum),
        volumeSum = (x.volumeSum ++ y.volumeSum).reduceOption(_ + _),
        volumeWeightedTaskDurationSum =
          (x.volumeWeightedTaskDurationSum ++ y.volumeWeightedTaskDurationSum).reduceOption(_.plus(_))
      )
    }
  }
}

private class StatisticsServiceLive(private val statisticsRepository: StatisticsRepository.Service)
    extends StatisticsService.Service {

  override def read(params: StatisticsParams): IO[RepositoryThrowable, StatisticsOutput] = {
    for {
      entities <- statisticsRepository.list(params)
      statistics = entities.map(toStatistics).combineAll
    } yield toOutput(statistics)
  }

  private def toStatistics(entity: StatisticsEntity) =
    Statistics(
      numberOfTasks = entity.numberOfTasks,
      numberOfTasksWithVolume = entity.numberOfTasksWithVolume,
      durationSum = entity.durationSum,
      volumeSum = entity.volumeSum,
      volumeWeightedTaskDurationSum = entity.volumeWeightedTaskDurationSum
    )

  private def toOutput(statistics: Statistics) =
    StatisticsOutput(
      numberOfTasks = statistics.numberOfTasks,
      averageTaskDuration = Option.when(statistics.numberOfTasks > 0) {
        statistics.durationSum.dividedBy(statistics.numberOfTasks)
      },
      averageTaskVolume = for {
        volumeSum               <- statistics.volumeSum
        numberOfTasksWithVolume <- statistics.numberOfTasksWithVolume
      } yield (volumeSum / BigDecimal(numberOfTasksWithVolume)).setScale(2, RoundingMode.HALF_UP),
      volumeWeightedAverageTaskDuration = for {
        volumeWeightedTaskDurationSum <- statistics.volumeWeightedTaskDurationSum
        volumeSum                     <- statistics.volumeSum
      } yield volumeWeightedTaskDurationSum.dividedBy(volumeSum).truncatedTo(ChronoUnit.SECONDS)
    )
}
