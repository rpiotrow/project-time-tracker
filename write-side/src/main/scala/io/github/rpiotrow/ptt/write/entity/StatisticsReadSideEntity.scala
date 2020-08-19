package io.github.rpiotrow.ptt.write.entity

import java.time.Duration
import java.util.UUID

case class StatisticsReadSideEntity(
  dbId: Long,
  owner: UUID,
  year: Int,
  month: Int,
  numberOfTasks: Int,
  numberOfTasksWithVolume: Int,
  durationSum: Duration,
  volumeWeightedTaskDurationSum: Option[Duration],
  volumeSum: Option[BigDecimal]
)
