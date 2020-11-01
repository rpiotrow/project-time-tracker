package io.github.rpiotrow.ptt.write.entity

import java.time.{Duration, Instant}

import io.github.rpiotrow.ptt.api.model.{TaskId, UserId}

case class TaskEntity(
  dbId: Long,
  taskId: TaskId,
  projectDbId: Long,
  createdAt: Instant,
  deletedAt: Option[Instant],
  owner: UserId,
  startedAt: Instant,
  duration: Duration,
  volume: Option[Int],
  comment: Option[String]
)
