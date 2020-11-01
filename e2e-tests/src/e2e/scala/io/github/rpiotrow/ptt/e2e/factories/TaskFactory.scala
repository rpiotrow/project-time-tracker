package io.github.rpiotrow.ptt.e2e.factories

import java.time.{Duration, OffsetDateTime}
import java.util.UUID

import eu.timepit.refined
import io.github.rpiotrow.ptt.api.input.TaskInput
import io.github.rpiotrow.ptt.api.model.{ProjectId, TaskId}
import io.github.rpiotrow.ptt.api.output.LocationHeader
import io.github.rpiotrow.ptt.e2e.configuration.End2EndTestsConfiguration.config

import scala.util.Random

object TaskFactory {
  def generateTaskInput(): TaskInput = {
    TaskInput(
      startedAt = OffsetDateTime.now().minusMinutes(Random.nextInt(1000)),
      duration = Duration.ofMinutes(Random.between(1, 8 * 60)),
      volume = Some(Random.between(1, 10)),
      comment = Some(Random.nextString(Random.between(1, 64)))
    )
  }
  def extractTaskId(location: LocationHeader, projectId: ProjectId): TaskId = {
    TaskId(
      UUID.fromString(location.toString.substring(s"${config.application.baseUri}/projects/$projectId/tasks/".length))
    )
  }
}
