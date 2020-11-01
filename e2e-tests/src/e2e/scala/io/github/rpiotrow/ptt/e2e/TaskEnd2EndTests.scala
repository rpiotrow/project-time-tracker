package io.github.rpiotrow.ptt.e2e

import java.time.{Duration, OffsetDateTime}

import io.github.rpiotrow.ptt.api.error._
import io.github.rpiotrow.ptt.api.model._
import io.github.rpiotrow.ptt.e2e.configuration.End2EndTestsConfiguration.config
import io.github.rpiotrow.ptt.e2e.factories.ProjectFactory._
import io.github.rpiotrow.ptt.e2e.factories.TaskFactory._
import io.github.rpiotrow.ptt.e2e.factories.UserIdFactory._
import io.github.rpiotrow.ptt.e2e.utils._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class TaskEnd2EndTests extends AnyFunSpec with should.Matchers with End2EndTestsBase with ApiResults {

  import ApiClient._

  describe("task scenarios") {
    it("add task") {
      val (projectId: ProjectId, userId: UserId) = createProjectWithOwner

      val taskInput   = generateTaskInput()
      val createdTask = createTask(projectId, taskInput, userId).success()
      createdTask.toString should startWith(s"${config.application.baseUri}/projects/$projectId/tasks/")

      val notExistingProjectId = generateProjectId()
      val invalidProject       = createTask(notExistingProjectId, taskInput, userId).failure()
      invalidProject should be(NotFound(s"project '$notExistingProjectId' does not exist"))

      val theSameTimeSpan = createTask(projectId, taskInput, userId).failure()
      theSameTimeSpan should be(Conflict("other task overlaps task time span"))

      val differentUserId: UserId    = generateUserId()
      val createdTaskByDifferentUser = createTask(projectId, taskInput, differentUserId).success()
      createdTaskByDifferentUser.toString should startWith(
        s"${config.application.baseUri}/projects/${projectId.value}/tasks/"
      )
    }
    it("update task") {
      val (projectId: ProjectId, userId: UserId) = createProjectWithOwner

      val taskInput = generateTaskInput()
      val taskId    = extractTaskId(createTask(projectId, taskInput, userId).success(), projectId)

      val taskUpdatedNotOwner =
        updateTask(projectId, taskId, taskInput.copy(volume = None, comment = Some("c")), generateUserId()).failure()
      taskUpdatedNotOwner should be(Forbidden("only owner can update task"))

      val taskUpdated   =
        updateTask(projectId, taskId, taskInput.copy(volume = None, comment = Some("c")), userId).success()
      val taskUpdatedId = extractTaskId(taskUpdated, projectId)

      val projectTasks = projectDetail(projectId, userId).success().tasks
      projectTasks.map(_.taskId) should contain only (taskId, taskUpdatedId)

      val notExistingProjectId       = generateProjectId()
      val taskUpdateInvalidProjectId = updateTask(notExistingProjectId, taskUpdatedId, taskInput, userId).failure()
      taskUpdateInvalidProjectId should be(NotFound(s"project '${notExistingProjectId}' does not exist"))

      deleteTask(projectId, taskUpdatedId, userId).success()

      val projectDeletedTasks = projectDetail(projectId, userId).success().tasks
      projectDeletedTasks should have size 2

      val taskUpdateDeleted = updateTask(projectId, taskUpdatedId, taskInput, userId).failure()
      val deletedAt         = projectDeletedTasks.filter(_.taskId == taskUpdatedId).head.deletedAt.get
      taskUpdateDeleted should be(Conflict(s"task ${taskUpdatedId.id} deleted at $deletedAt"))
    }
    it("update task with overlapping time") {
      val (projectId: ProjectId, userId: UserId) = createProjectWithOwner

      val taskInput =
        generateTaskInput().copy(
          startedAt = OffsetDateTime.parse("2020-09-10T09:00:00Z"),
          duration = Duration.ofHours(8)
        )
      createTask(projectId, taskInput, userId).success()

      val taskInputOverlapBegin       =
        taskInput.copy(startedAt = OffsetDateTime.parse("2020-09-10T08:00:00Z"), duration = Duration.ofHours(2))
      val taskInputOverlapEnd         =
        taskInput.copy(startedAt = OffsetDateTime.parse("2020-09-10T16:00:00Z"), duration = Duration.ofHours(2))
      val taskInputOverlapInTheMiddle =
        taskInput.copy(startedAt = OffsetDateTime.parse("2020-09-10T12:00:00Z"), duration = Duration.ofHours(2))
      List(taskInputOverlapBegin, taskInputOverlapEnd, taskInputOverlapInTheMiddle).foreach(t => {
        val taskOverlapping = createTask(projectId, t, userId).failure()
        taskOverlapping should be(Conflict("other task overlaps task time span"))
      })

      val differentUser = generateUserId()
      List(taskInputOverlapBegin, taskInputOverlapEnd, taskInputOverlapInTheMiddle).foreach(t => {
        createTask(projectId, t, differentUser).success()
      })
    }
    it("delete task") {
      val (projectId: ProjectId, userId: UserId) = createProjectWithOwner

      val taskInput = generateTaskInput()
      val taskId    = extractTaskId(createTask(projectId, taskInput, userId).success(), projectId)

      val differentUser = generateUserId()
      val notOwner      = deleteTask(projectId, taskId, differentUser).failure()
      notOwner should be(Forbidden("only owner can delete task"))

      val differentProjectId = generateProjectId()
      val invalidProjectId   = deleteTask(differentProjectId, taskId, userId).failure()
      invalidProjectId should be(NotFound(s"project '$differentProjectId' does not exist"))

      deleteTask(projectId, taskId, userId).success()

      val projectDeletedTasks = projectDetail(projectId, userId).success().tasks
      val deletedAt           = projectDeletedTasks.head.deletedAt.get

      val alreadyDeleted = deleteTask(projectId, taskId, userId).failure()
      alreadyDeleted should be(Conflict(s"task ${taskId.id} deleted at $deletedAt"))
    }
  }

}
