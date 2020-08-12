package io.github.rpiotrow.ptt.api.output

import java.time.{Duration, LocalDateTime}

import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}

case class ProjectOutput(
  projectId: String,
  createdAt: LocalDateTime,
  owner: UserId,
  durationSum: Duration,
  tasks: List[TaskOutput]
)
