package io.github.rpiotrow.ptt.e2e

import io.github.rpiotrow.ptt.api.error.ApiError
import io.github.rpiotrow.ptt.api.input.ProjectInput
import io.github.rpiotrow.ptt.api.model.ProjectId
import io.github.rpiotrow.ptt.api.output.LocationHeader
import io.github.rpiotrow.ptt.e2e.configuration.End2EndTestsConfiguration.config
import io.github.rpiotrow.ptt.e2e.factories.ProjectFactory.generateProjectId
import io.github.rpiotrow.ptt.e2e.factories.TaskFactory.generateTaskInput
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class End2EndTests extends AnyFunSpec with should.Matchers with EitherValues {

  import ApiClient._

  private implicit class ApiResultOps[A](result: Either[ApiError, A]) {
    def success(): A = {
      result.fold(apiError => fail(apiError.toString), identity)
    }
  }

  describe("project scenarios") {
    it("create project with task") {
      val projectId: ProjectId = generateProjectId()
      val createProjectResult  = createProject(new ProjectInput(projectId)).success()
      createProjectResult should be(new LocationHeader(s"${config.application.baseUri}/projects/${projectId.value}"))

      createTask(projectId, generateTaskInput()).success()
    }
  }
}
