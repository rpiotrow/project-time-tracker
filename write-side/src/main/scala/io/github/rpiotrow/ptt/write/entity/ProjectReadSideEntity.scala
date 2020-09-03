package io.github.rpiotrow.ptt.write.entity

import java.time.{Duration, LocalDateTime}
import java.util.UUID

import io.github.rpiotrow.ptt.api.model.UserId

case class ProjectReadSideEntity(
  dbId: Long,
  projectId: String,
  createdAt: LocalDateTime,
  lastAddDurationAt: LocalDateTime,
  deletedAt: Option[LocalDateTime],
  owner: UserId,
  durationSum: Duration
)

object ProjectReadSideEntity {
  def apply(project: ProjectEntity): ProjectReadSideEntity = {
    ProjectReadSideEntity(
      dbId = 0,
      projectId = project.projectId,
      createdAt = project.createdAt,
      lastAddDurationAt = project.createdAt,
      deletedAt = project.deletedAt,
      owner = project.owner,
      durationSum = Duration.ZERO
    )
  }
}
