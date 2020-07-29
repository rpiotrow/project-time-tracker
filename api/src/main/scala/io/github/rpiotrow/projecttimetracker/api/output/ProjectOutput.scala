package io.github.rpiotrow.projecttimetracker.api.output

import java.time.{Duration, LocalDateTime}

import io.github.rpiotrow.projecttimetracker.api.Model.{ProjectId, UserId}

case class ProjectOutput(
  id: String,
  createdAt: LocalDateTime,
  owner: UserId,
  durationSum: Duration,
  tasks: List[TaskOutput]
)
