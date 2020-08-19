package io.github.rpiotrow.ptt.read.entity

import java.time.Duration
import java.util.UUID

case class StatisticsEntity(
  owner: UUID,
  year: Int,
  month: Int,
  numberOfTasks: Int,
  numberOfTasksWithVolume: Option[Int],
  durationSum: Duration,
  volumeSum: Option[Long],
  volumeWeightedTaskDurationSum: Option[Duration]
)
