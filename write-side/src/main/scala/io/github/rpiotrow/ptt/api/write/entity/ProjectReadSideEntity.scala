package io.github.rpiotrow.ptt.api.write.entity

import java.time.{Duration, LocalDateTime}
import java.util.UUID

case class ProjectReadSideEntity(
  dbId: Long,
  projectId: String,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime,
  deletedAt: Option[LocalDateTime],
  owner: UUID,
  durationSum: Duration
)

object ProjectReadSideEntity {
  def apply(project: ProjectEntity): ProjectReadSideEntity = {
    ProjectReadSideEntity(
      dbId = 0,
      projectId = project.projectId,
      createdAt = project.createdAt,
      updatedAt = project.updatedAt,
      deletedAt = project.deletedAt,
      owner = project.owner,
      durationSum = Duration.ZERO
    )
  }
}