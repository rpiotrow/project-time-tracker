package io.github.rpiotrow.ptt.write.entity

import java.time.{Duration, Instant}

import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}

case class ProjectReadSideEntity(
  dbId: Long,
  projectId: ProjectId,
  createdAt: Instant,
  lastAddDurationAt: Instant,
  deletedAt: Option[Instant],
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
