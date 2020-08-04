package io.github.rpiotrow.ptt.api.output

import java.time.Duration

case class StatisticsOutput(
  numberOfTasks: Int,
  averageTaskDuration: Duration,
  averageTaskVolume: BigDecimal,
  volumeWeightedAverageTaskDuration: Duration
)

object StatisticsOutput {
  val ZERO = StatisticsOutput(0, Duration.ZERO, 0, Duration.ZERO)
}
