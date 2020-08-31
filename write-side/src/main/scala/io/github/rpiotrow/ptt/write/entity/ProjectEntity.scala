package io.github.rpiotrow.ptt.write.entity

import java.time.LocalDateTime
import java.util.UUID

case class ProjectEntity(
  dbId: Long = 0L,
  projectId: String,
  createdAt: LocalDateTime,
  deletedAt: Option[LocalDateTime],
  owner: UUID
)
