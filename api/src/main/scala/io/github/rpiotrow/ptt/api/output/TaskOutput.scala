package io.github.rpiotrow.ptt.api.output

import java.time.{Duration, ZoneOffset, OffsetDateTime}

import cats.implicits._
import io.github.rpiotrow.ptt.api.model._

case class TaskOutput(
  taskId: TaskId,
  owner: UserId,
  startedAt: OffsetDateTime,
  duration: Duration,
  volume: Option[Int],
  comment: Option[String],
  deletedAt: Option[OffsetDateTime]
)

object TaskOutput {
  private[api] val example = TaskOutput(
    taskId = TaskId.example,
    owner = UserId.example,
    startedAt = OffsetDateTime.now(ZoneOffset.UTC),
    duration = Duration.ofMinutes(30),
    volume = 10.some,
    comment = "First task".some,
    deletedAt = None
  )
}
