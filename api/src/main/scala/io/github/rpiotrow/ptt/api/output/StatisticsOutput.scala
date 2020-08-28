package io.github.rpiotrow.ptt.api.output

import cats.implicits._
import java.time.Duration

case class StatisticsOutput(
  numberOfTasks: Int,
  averageTaskDuration: Option[Duration],
  averageTaskVolume: Option[BigDecimal],
  volumeWeightedAverageTaskDuration: Option[Duration]
)

object StatisticsOutput {
  val ZERO = StatisticsOutput(0, None, None, None)

  private[api] val example = StatisticsOutput(
    numberOfTasks = 5,
    averageTaskDuration = Duration.ofMinutes(32).some,
    averageTaskVolume = BigDecimal(10.5).some,
    volumeWeightedAverageTaskDuration = Duration.ofMinutes(44).some
  )
}
