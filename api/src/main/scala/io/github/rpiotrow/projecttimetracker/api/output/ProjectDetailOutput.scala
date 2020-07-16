package io.github.rpiotrow.projecttimetracker.api.output

import java.time.LocalDateTime

import io.github.rpiotrow.projecttimetracker.api.Model.{ProjectId, UserId}

case class ProjectDetailOutput(id: ProjectId, createdAt: LocalDateTime, owner: UserId, tasks: List[TaskOutput])
