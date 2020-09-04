package io.github.rpiotrow.ptt.write.entity

import java.time.LocalDateTime

import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}

case class ProjectEntity(
  dbId: Long = 0L,
  projectId: ProjectId,
  createdAt: LocalDateTime,
  deletedAt: Option[LocalDateTime],
  owner: UserId
)
