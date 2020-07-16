package io.github.rpiotrow.projecttimetracker.api.output

import java.time.{LocalDateTime, Duration}

import io.github.rpiotrow.projecttimetracker.api.Model._

case class TaskOutput(
  owner: UserId,
  startedAt: LocalDateTime,
  duration: Duration,
  volume: Option[Int],
  comment: Option[String]
)
