package io.github.rpiotrow.ptt.api.input

import eu.timepit.refined.auto._
import io.github.rpiotrow.ptt.api.model.ProjectId

case class ProjectInput(projectId: ProjectId)

object ProjectInput {
  private[api] val example = ProjectInput("awesome-project-one")
}
