package io.github.rpiotrow.ptt.api.output

import java.time.{LocalDateTime, Duration}

import io.github.rpiotrow.ptt.api.model._

case class TaskOutput(
  taskId: TaskId,
  owner: UserId,
  startedAt: LocalDateTime,
  duration: Duration,
  volume: Option[Int],
  comment: Option[String]
)
