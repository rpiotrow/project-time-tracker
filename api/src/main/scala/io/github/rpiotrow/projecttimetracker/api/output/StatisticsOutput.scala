package io.github.rpiotrow.projecttimetracker.api.output

case class StatisticsOutput(
  numberOfTasks: Int,
  averageTaskDuration: Double,
  averageTaskVolume: Double,
  averageTaskDurationWeightedVolume: Double
)
