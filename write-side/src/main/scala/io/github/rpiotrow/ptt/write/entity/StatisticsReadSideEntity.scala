package io.github.rpiotrow.ptt.write.entity

import java.time.Duration
import java.util.UUID

import io.github.rpiotrow.ptt.api.model.UserId

case class StatisticsReadSideEntity(
  dbId: Long,
  owner: UserId,
  year: Int,
  month: Int,
  numberOfTasks: Int,
  numberOfTasksWithVolume: Option[Int],
  durationSum: Duration,
  volumeSum: Option[Long],
  volumeWeightedTaskDurationSum: Option[Duration]
)
