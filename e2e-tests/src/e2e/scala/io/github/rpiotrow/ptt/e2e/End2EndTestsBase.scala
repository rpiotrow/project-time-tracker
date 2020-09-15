package io.github.rpiotrow.ptt.e2e

import io.github.rpiotrow.ptt.api.input.ProjectInput
import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}
import io.github.rpiotrow.ptt.e2e.factories.ProjectFactory.generateProjectId
import io.github.rpiotrow.ptt.e2e.factories.UserIdFactory.generateUserId
import io.github.rpiotrow.ptt.e2e.utils.ApiClient.createProject
import io.github.rpiotrow.ptt.e2e.utils.ApiResults

trait End2EndTestsBase { this: ApiResults =>

  protected def createProjectWithOwner: (ProjectId, UserId) = {
    val ownerId   = generateUserId()
    val projectId = generateProjectId()
    createProject(new ProjectInput(projectId), ownerId).success()
    (projectId, ownerId)
  }

}
