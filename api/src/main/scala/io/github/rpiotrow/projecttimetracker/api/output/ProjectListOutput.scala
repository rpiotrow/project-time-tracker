package io.github.rpiotrow.projecttimetracker.api.output

import java.time.LocalDateTime

import io.github.rpiotrow.projecttimetracker.api.Model._

case class ProjectListOutput(id: ProjectId, createdAt: LocalDateTime, owner: UserId)
