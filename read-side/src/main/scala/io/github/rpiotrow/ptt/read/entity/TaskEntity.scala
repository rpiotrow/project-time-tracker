package io.github.rpiotrow.ptt.read.entity

import java.time.{Duration, LocalDateTime}
import java.util.UUID

case class TaskEntity(
  projectId: Long,
  deletedAt: Option[LocalDateTime],
  owner: UUID,
  startedAt: LocalDateTime,
  duration: Duration,
  volume: Option[Int],
  comment: Option[String]
)
