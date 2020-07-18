package io.github.rpiotrow.ptt.read.entity

import java.util.UUID

case class StatisticsEntity(
  dbId: Long,
  owner: UUID,
  year: Int,
  month: Int,
  numberOfTasks: Int,
  averageTaskDuration: Double,
  averageTaskVolume: Double,
  volumeWeightedAverageTaskDuration: Double
)
