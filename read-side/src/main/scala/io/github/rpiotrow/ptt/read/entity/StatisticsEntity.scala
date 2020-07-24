package io.github.rpiotrow.ptt.read.entity

import java.time.Duration
import java.util.UUID

case class StatisticsEntity(
  owner: UUID,
  year: Int,
  month: Int,
  numberOfTasks: Int,
  averageTaskDuration: Duration,
  averageTaskVolume: Double,
  volumeWeightedAverageTaskDuration: Duration
)
