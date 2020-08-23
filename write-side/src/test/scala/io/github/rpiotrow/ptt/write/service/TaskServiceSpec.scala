package io.github.rpiotrow.ptt.write.service

import java.util.UUID

import cats.implicits._
import cats.Monad
import com.softwaremill.diffx.scalatest.DiffMatcher.matchTo
import io.github.rpiotrow.ptt.api.output.TaskOutput
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, TaskEntity}
import io.github.rpiotrow.ptt.write.repository.{DBResult, ProjectRepository, TaskRepository}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class TaskServiceSpec extends AnyFunSpec with ServiceSpecBase with MockFactory with should.Matchers {

  describe("TaskService") {
    describe("add should") {
      it("create new task") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive(taskRepository, projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId.value).returning(Option(project).pure[DBResult])
        (taskRepository.overlapping _)
          .expects(userId, taskInput.startedAt, taskInput.duration)
          .returning(List[TaskEntity]().pure[DBResult])
        (taskRepository.add _).expects(project.dbId, taskInput, userId).returning(task.pure[DBResult])
        (readSideService.taskAdded _)
          .expects(task)
          .returning(taskReadModel.pure[DBResult])

        val result = service.add(projectId, taskInput, userId).value.unsafeRunSync()

        result should matchTo(taskOutput.asRight[AppFailure])
      }
      it("do not create task when it overlaps another one") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive(taskRepository, projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId.value).returning(Option(project).pure[DBResult])
        (taskRepository.overlapping _)
          .expects(userId, taskInput.startedAt, taskInput.duration)
          .returning(List[TaskEntity](task.copy(dbId = 22)).pure[DBResult])

        val result = service.add(projectId, taskInput, userId).value.unsafeRunSync()

        result should be(InvalidTimeSpan.asLeft[TaskOutput])
      }
      it("do not create task for non-existing project") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive(taskRepository, projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId.value).returning(Option.empty[ProjectEntity].pure[DBResult])

        val result = service.add(projectId, taskInput, ownerId).value.unsafeRunSync()

        result should be(EntityNotFound("project with given identifier does not exist").asLeft[TaskOutput])
      }
    }

    describe("update should") {
      it("soft delete current task and create new one") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive(taskRepository, projectRepository, readSideService, tnx)

        (taskRepository.get _).expects(taskId).returning(Option(task).pure[DBResult])
        (taskRepository.overlapping _)
          .expects(ownerId, updateInput.startedAt, updateInput.duration)
          .returning(List[TaskEntity]().pure[DBResult])
        (taskRepository.delete _).expects(task).returning(task.pure[DBResult])
        (readSideService.taskDeleted _)
          .expects(task)
          .returning(Monad[DBResult].unit)
        (taskRepository.add _).expects(project.dbId, updateInput, ownerId).returning(taskUpdated.pure[DBResult])
        (readSideService.taskAdded _)
          .expects(taskUpdated)
          .returning(taskUpdatedReadModel.pure[DBResult])

        val result = service.update(taskId, updateInput, ownerId).value.unsafeRunSync()

        result should be(taskUpdatedOutput.asRight[AppFailure])
      }
      it("return error when task with given identifier does not exist") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive(taskRepository, projectRepository, readSideService, tnx)

        (taskRepository.get _).expects(taskId).returning(Option.empty[TaskEntity].pure[DBResult])

        val result = service.update(taskId, updateInput, ownerId).value.unsafeRunSync()

        result should be(EntityNotFound("task with given identifier does not exist").asLeft[Unit])
      }
      it("return error when not owner of the task wants to delete it") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive(taskRepository, projectRepository, readSideService, tnx)

        (taskRepository.get _).expects(taskId).returning(Option(task).pure[DBResult])

        val notOwnerId = UUID.randomUUID()
        val result     = service.update(taskId, updateInput, notOwnerId).value.unsafeRunSync()

        result should be(NotOwner("only owner can delete task").asLeft[Unit])
      }
      it("do not update when new time range overlaps another task time span") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive(taskRepository, projectRepository, readSideService, tnx)

        (taskRepository.get _).expects(taskId).returning(Option(task).pure[DBResult])
        (taskRepository.overlapping _)
          .expects(ownerId, updateInput.startedAt, updateInput.duration)
          .returning(List[TaskEntity](task.copy(dbId = 22)).pure[DBResult])

        val result = service.update(taskId, updateInput, ownerId).value.unsafeRunSync()

        result should be(InvalidTimeSpan.asLeft[TaskOutput])
      }
    }

    describe("delete should") {
      it("soft task project on write side and update read side") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive(taskRepository, projectRepository, readSideService, tnx)

        (taskRepository.get _).expects(taskId).returning(Option(task).pure[DBResult])
        (taskRepository.delete _).expects(task).returning(task.pure[DBResult])
        (readSideService.taskDeleted _)
          .expects(task)
          .returning(Monad[DBResult].unit)

        val result = service.delete(taskId, ownerId).value.unsafeRunSync()

        result should be(().asRight[AppFailure])
      }
      it("return error when project with given identifier does not exist") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive(taskRepository, projectRepository, readSideService, tnx)

        (taskRepository.get _).expects(taskId).returning(Option.empty[TaskEntity].pure[DBResult])

        val result = service.delete(taskId, ownerId).value.unsafeRunSync()

        result should be(EntityNotFound("task with given identifier does not exist").asLeft[Unit])
      }
      it("return error when not owner of the task wants to delete it") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive(taskRepository, projectRepository, readSideService, tnx)

        (taskRepository.get _).expects(taskId).returning(Option(task).pure[DBResult])

        val notOwnerId = UUID.randomUUID()
        val result     = service.delete(taskId, notOwnerId).value.unsafeRunSync()

        result should be(NotOwner("only owner can delete task").asLeft[Unit])
      }
    }
  }
}
