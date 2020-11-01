package io.github.rpiotrow.ptt.write.entity

import java.time.Instant

import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}

case class ProjectEntity(
  dbId: Long = 0L,
  projectId: ProjectId,
  createdAt: Instant,
  deletedAt: Option[Instant],
  owner: UserId
)
