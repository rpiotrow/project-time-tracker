package io.github.rpiotrow.ptt.read.entity

import java.time.{Duration, LocalDateTime}

import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}

case class ProjectEntity(
  dbId: Long,
  projectId: ProjectId,
  createdAt: LocalDateTime,
  lastAddDurationAt: LocalDateTime,
  deletedAt: Option[LocalDateTime],
  owner: UserId,
  durationSum: Duration
)
