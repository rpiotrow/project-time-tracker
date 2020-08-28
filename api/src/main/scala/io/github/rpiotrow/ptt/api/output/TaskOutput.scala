package io.github.rpiotrow.ptt.api.output

import java.time.{Duration, LocalDateTime}
import java.util.UUID

import cats.implicits._
import io.github.rpiotrow.ptt.api.model._

case class TaskOutput(
  taskId: TaskId,
  owner: UserId,
  startedAt: LocalDateTime,
  duration: Duration,
  volume: Option[Int],
  comment: Option[String],
  deletedAt: Option[LocalDateTime]
)

object TaskOutput {
  private[api] val example = TaskOutput(
    taskId = TaskId.example,
    owner = UserId.example,
    startedAt = LocalDateTime.now(),
    duration = Duration.ofMinutes(30),
    volume = 10.some,
    comment = "First task".some,
    deletedAt = None
  )
}
