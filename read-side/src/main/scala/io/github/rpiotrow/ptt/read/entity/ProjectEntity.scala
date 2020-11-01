package io.github.rpiotrow.ptt.read.entity

import java.time.{Duration, Instant}

import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}

case class ProjectEntity(
  dbId: Long,
  projectId: ProjectId,
  createdAt: Instant,
  lastAddDurationAt: Instant,
  deletedAt: Option[Instant],
  owner: UserId,
  durationSum: Duration
)
