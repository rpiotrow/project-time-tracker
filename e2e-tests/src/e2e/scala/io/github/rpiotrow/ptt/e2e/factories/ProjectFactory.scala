package io.github.rpiotrow.ptt.e2e.factories

import java.util.UUID

import eu.timepit.refined
import io.github.rpiotrow.ptt.api.model.ProjectId

object ProjectFactory {
  def generateProjectId(): ProjectId = {
    val either: Either[String, ProjectId] =
      refined.refineV("project-" + UUID.randomUUID().toString)
    either.fold(err => throw new IllegalArgumentException(err), identity)
  }
}
