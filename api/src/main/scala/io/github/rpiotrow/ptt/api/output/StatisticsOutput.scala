package io.github.rpiotrow.ptt.api.output

import java.time.Duration

case class StatisticsOutput(
  numberOfTasks: Int,
  averageTaskDuration: Option[Duration],
  averageTaskVolume: Option[BigDecimal],
  volumeWeightedAverageTaskDuration: Option[Duration]
)

object StatisticsOutput {
  val ZERO = StatisticsOutput(0, None, None, None)
}
