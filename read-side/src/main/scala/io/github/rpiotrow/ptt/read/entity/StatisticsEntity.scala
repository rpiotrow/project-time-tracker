package io.github.rpiotrow.ptt.read.entity

import java.util.UUID

case class StatisticsEntity(
  owner: UUID,
  year: Int,
  month: Int,
  numberOfTasks: Int,
  averageTaskDurationMinutes: Int,
  averageTaskVolume: BigDecimal,
  // nominator of volume weighted average task duration
  volumeWeightedTaskDurationSum: BigDecimal,
  // denominator of volume weighted average task duration
  volumeSum: BigDecimal
)
