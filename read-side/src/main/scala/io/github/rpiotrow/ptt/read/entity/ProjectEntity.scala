package io.github.rpiotrow.ptt.read.entity

import java.time.{Duration, LocalDateTime}
import java.util.UUID

case class ProjectEntity(
  dbId: Long,
  projectId: String,
  createdAt: LocalDateTime,
  lastAddDurationAt: LocalDateTime,
  deletedAt: Option[LocalDateTime],
  owner: UUID,
  durationSum: Duration
)
