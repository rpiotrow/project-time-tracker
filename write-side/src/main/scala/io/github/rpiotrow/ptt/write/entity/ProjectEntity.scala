package io.github.rpiotrow.ptt.write.entity

import java.time.LocalDateTime
import java.util.UUID

import io.github.rpiotrow.ptt.api.model.UserId

case class ProjectEntity(
  dbId: Long = 0L,
  projectId: String,
  createdAt: LocalDateTime,
  deletedAt: Option[LocalDateTime],
  owner: UserId
)
