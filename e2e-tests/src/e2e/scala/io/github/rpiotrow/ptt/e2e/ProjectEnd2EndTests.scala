package io.github.rpiotrow.ptt.e2e

import io.github.rpiotrow.ptt.api.error._
import io.github.rpiotrow.ptt.api.input._
import io.github.rpiotrow.ptt.api.model._
import io.github.rpiotrow.ptt.api.output._
import io.github.rpiotrow.ptt.e2e.configuration.End2EndTestsConfiguration.config
import io.github.rpiotrow.ptt.e2e.factories.ProjectFactory._
import io.github.rpiotrow.ptt.e2e.factories.TaskFactory._
import io.github.rpiotrow.ptt.e2e.factories.UserIdFactory._
import io.github.rpiotrow.ptt.e2e.utils._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class ProjectEnd2EndTests extends AnyFunSpec with should.Matchers with End2EndTestsBase with ApiResults {

  import ApiClient._

  describe("project scenarios") {
    it("create project with task") {
      val userId    = generateUserId()
      val projectId = generateProjectId()

      val createdProject = createProject(new ProjectInput(projectId), userId).success()
      createdProject should be(new LocationHeader(s"${config.application.baseUri}/projects/${projectId.value}"))

      val taskInput = generateTaskInput()
      createTask(projectId, taskInput, userId).success()

      val projectOutput = projectDetail(projectId, userId).success()
      projectOutput.projectId should be(projectId)
      projectOutput.deletedAt should be(None)
      projectOutput.owner should be(userId)
      projectOutput.tasks should have size 1
      projectOutput.tasks.head.duration should be(taskInput.duration)

      val differentUserId = generateUserId()
      projectDetail(projectId, differentUserId).success()

      val notExisting        = generateProjectId()
      val notExistingProject = projectDetail(notExisting, userId).failure()
      notExistingProject should be(NotFound(s"project '$notExisting' not found"))
    }
    it("update project") {
      val userId    = generateUserId()
      val projectId = createProjectWithTasks(userId)

      val newProjectId    = generateProjectId()
      val newProjectInput = new ProjectInput(newProjectId)

      val differentUserId = generateUserId()
      val notOwnerUpdate  = updateProject(projectId, newProjectInput, differentUserId).failure()
      notOwnerUpdate should be(Forbidden("only owner can update project"))

      val updateWithTheSameProjectId = updateProject(projectId, new ProjectInput(projectId), userId).failure()
      updateWithTheSameProjectId should be(Conflict(s"project '$projectId' already exists"))

      val updatedProject = updateProject(projectId, newProjectInput, userId).success()
      updatedProject should be(new LocationHeader(s"${config.application.baseUri}/projects/$newProjectId"))

      deleteProject(newProjectId, userId).success()

      val newerProjectId = generateProjectId()
      val updateDeleted  = updateProject(newProjectId, new ProjectInput(newerProjectId), userId).success()
      updateDeleted should be(new LocationHeader(s"${config.application.baseUri}/projects/$newerProjectId"))
    }
    it("delete empty project") {
      val (projectId: ProjectId, userId: UserId) = createProjectWithOwner

      invokeDeleteProject(userId, projectId)
    }
    it("delete project with tasks") {
      val userId    = generateUserId()
      val projectId = createProjectWithTasks(userId)

      invokeDeleteProject(userId, projectId)
    }
    it("do not create two projects with the same project id") {
      val userId       = generateUserId()
      val projectId    = generateProjectId()
      val projectInput = new ProjectInput(projectId)
      createProject(projectInput, userId).success()

      val apiError = createProject(projectInput, userId).failure()
      apiError should be(Conflict(s"project '$projectId' already exists"))
    }
  }

  private def createProjectWithTasks(userId: UserId): ProjectId = {
    val projectId: ProjectId    = generateProjectId()
    createProject(new ProjectInput(projectId), userId).success()
    createTask(projectId, generateTaskInput(), userId).success()
    val differentUserId: UserId = generateUserId()
    createTask(projectId, generateTaskInput(), differentUserId).success()
    projectId
  }

  private def invokeDeleteProject(userId: UserId, projectId: ProjectId) = {
    val differentUserId = generateUserId()
    val notOwnerDelete  = deleteProject(projectId, differentUserId).failure()
    notOwnerDelete should be(Forbidden("only owner can delete project"))

    deleteProject(projectId, userId).success()

    val projectOutputDeleted = projectDetail(projectId, userId).success()
    projectOutputDeleted.deletedAt.isDefined should be(true)

    val alreadyDeleted = deleteProject(projectId, userId).failure()
    alreadyDeleted should be(Conflict(s"project '$projectId' deleted at ${projectOutputDeleted.deletedAt.get}"))
  }

}
