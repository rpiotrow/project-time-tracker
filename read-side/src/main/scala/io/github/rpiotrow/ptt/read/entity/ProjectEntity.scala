package io.github.rpiotrow.ptt.read.entity

import java.time.{Duration, LocalDateTime}
import java.util.UUID

import io.github.rpiotrow.ptt.api.model.UserId

case class ProjectEntity(
  dbId: Long,
  projectId: String,
  createdAt: LocalDateTime,
  lastAddDurationAt: LocalDateTime,
  deletedAt: Option[LocalDateTime],
  owner: UserId,
  durationSum: Duration
)
