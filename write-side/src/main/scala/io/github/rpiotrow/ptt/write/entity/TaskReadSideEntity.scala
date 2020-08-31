package io.github.rpiotrow.ptt.write.entity

import java.time.{Duration, LocalDateTime}

import io.github.rpiotrow.ptt.api.model.{TaskId, UserId}

case class TaskReadSideEntity(
  dbId: Long,
  taskId: TaskId,
  projectDbId: Long,
  deletedAt: Option[LocalDateTime],
  owner: UserId,
  startedAt: LocalDateTime,
  duration: Duration,
  volume: Option[Int],
  comment: Option[String]
)

object TaskReadSideEntity {
  def apply(task: TaskEntity, projectDbId: Long): TaskReadSideEntity =
    TaskReadSideEntity(
      dbId = 0,
      taskId = task.taskId,
      projectDbId = projectDbId,
      deletedAt = task.deletedAt,
      owner = task.owner,
      startedAt = task.startedAt,
      duration = task.duration,
      volume = task.volume,
      comment = task.comment
    )
}
