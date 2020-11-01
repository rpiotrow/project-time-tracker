package io.github.rpiotrow.ptt.read.entity

import java.time.{Duration, Instant}

import io.github.rpiotrow.ptt.api.model.{TaskId, UserId}

case class TaskEntity(
  taskId: TaskId,
  projectDbId: Long,
  deletedAt: Option[Instant],
  owner: UserId,
  startedAt: Instant,
  duration: Duration,
  volume: Option[Int],
  comment: Option[String]
)
