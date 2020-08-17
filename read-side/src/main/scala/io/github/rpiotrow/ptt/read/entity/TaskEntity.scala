package io.github.rpiotrow.ptt.read.entity

import java.time.{Duration, LocalDateTime}

import io.github.rpiotrow.ptt.api.model.{TaskId, UserId}

case class TaskEntity(
  taskId: TaskId,
  projectDbId: Long,
  deletedAt: Option[LocalDateTime],
  owner: UserId,
  startedAt: LocalDateTime,
  duration: Duration,
  volume: Option[Int],
  comment: Option[String]
)
