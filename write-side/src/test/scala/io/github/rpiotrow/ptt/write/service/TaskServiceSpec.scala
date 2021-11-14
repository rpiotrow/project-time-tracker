package io.github.rpiotrow.ptt.write.service

import java.util.UUID

import eu.timepit.refined.auto._
import cats.effect.unsafe.implicits.global
import cats.implicits._
import cats.Monad
import cats.effect.IO
import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}
import io.github.rpiotrow.ptt.api.output.TaskOutput
import io.github.rpiotrow.ptt.write.entity._
import io.github.rpiotrow.ptt.write.repository._
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should


import java.time.Instant

class TaskServiceSpec extends AnyFunSpec with ServiceSpecBase with MockFactory with should.Matchers {

  describe("TaskService") {
    describe("add should") {
      it("create new task") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive[IO](taskRepository, projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId).returning(Option(project).pure[DBResult])
        (taskRepository.overlapping _)
          .expects(userId, taskInput.startedAt.toInstant, taskInput.duration)
          .returning(List[TaskEntity]().pure[DBResult])
        (taskRepository.add _).expects(project.dbId, taskInput, userId).returning(task.pure[DBResult])
        (readSideService.taskAdded _)
          .expects(projectId, task)
          .returning(Monad[DBResult].unit)

        val result = service.add(projectId, taskInput, userId).value.unsafeRunSync()

        result should be(taskId.asRight[AppFailure])
      }
      it("do not create task when it overlaps another one") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive[IO](taskRepository, projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId).returning(Option(project).pure[DBResult])
        (taskRepository.overlapping _)
          .expects(userId, taskInput.startedAt.toInstant, taskInput.duration)
          .returning(List[TaskEntity](task.copy(dbId = 22)).pure[DBResult])

        val result = service.add(projectId, taskInput, userId).value.unsafeRunSync()

        result should be(InvalidTimeSpan.asLeft[TaskOutput])
      }
      it("do not create task for non-existing project") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive[IO](taskRepository, projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId).returning(Option.empty[ProjectEntity].pure[DBResult])

        val result = service.add(projectId, taskInput, ownerId).value.unsafeRunSync()

        result should be(EntityNotFound(s"project '$projectId' does not exist").asLeft[TaskOutput])
      }
      it("do not create task for deleted project") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive[IO](taskRepository, projectRepository, readSideService, tnx)

        val now            = Instant.now()
        val deletedProject = project.copy(deletedAt = now.some)
        (projectRepository.get _).expects(projectId).returning(deletedProject.some.pure[DBResult])

        val result = service.add(projectId, taskInput, userId).value.unsafeRunSync()

        result should be(AlreadyDeleted(s"project '$projectId' deleted at $now").asLeft[TaskOutput])
      }
    }

    describe("update should") {
      it("soft delete current task and create new one") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive[IO](taskRepository, projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId).returning(Option(project).pure[DBResult])
        (taskRepository.get _).expects(taskId).returning(Option(task).pure[DBResult])
        (taskRepository.overlapping _)
          .expects(ownerId, updateInput.startedAt.toInstant, updateInput.duration)
          .returning(List[TaskEntity]().pure[DBResult])
        (taskRepository.delete _).expects(task).returning(task.pure[DBResult])
        (readSideService.taskDeleted _)
          .expects(task)
          .returning(Monad[DBResult].unit)
        (taskRepository.add _).expects(project.dbId, updateInput, ownerId).returning(taskUpdated.pure[DBResult])
        (readSideService.taskAdded _)
          .expects(projectId, taskUpdated)
          .returning(Monad[DBResult].unit)

        val result = service.update(projectId, taskId, updateInput, ownerId).value.unsafeRunSync()

        result should be(taskIdUpdated.asRight[AppFailure])
      }
      it("return error when task with given identifier does not exist") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive[IO](taskRepository, projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId).returning(Option(project).pure[DBResult])
        (taskRepository.get _).expects(taskId).returning(Option.empty[TaskEntity].pure[DBResult])

        val result = service.update(projectId, taskId, updateInput, ownerId).value.unsafeRunSync()

        result should be(EntityNotFound(s"task ${taskId.id} does not exist").asLeft[Unit])
      }
      it("return error when not owner of the task wants to delete it") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive[IO](taskRepository, projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId).returning(Option(project).pure[DBResult])
        (taskRepository.get _).expects(taskId).returning(Option(task).pure[DBResult])

        val notOwnerId = UserId(UUID.randomUUID())
        val result     = service.update(projectId, taskId, updateInput, notOwnerId).value.unsafeRunSync()

        result should be(NotOwner("only owner can update task").asLeft[Unit])
      }
      it("do not update when new time range overlaps another task time span") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive[IO](taskRepository, projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId).returning(Option(project).pure[DBResult])
        (taskRepository.get _).expects(taskId).returning(Option(task).pure[DBResult])
        (taskRepository.overlapping _)
          .expects(ownerId, updateInput.startedAt.toInstant, updateInput.duration)
          .returning(List[TaskEntity](task.copy(dbId = 22)).pure[DBResult])

        val result = service.update(projectId, taskId, updateInput, ownerId).value.unsafeRunSync()

        result should be(InvalidTimeSpan.asLeft[TaskOutput])
      }
      it("return error when task is not from given project") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive[IO](taskRepository, projectRepository, readSideService, tnx)

        val differentProjectId: ProjectId = "p1111"
        val differentProject              = project.copy(dbId = 21L, projectId = differentProjectId)
        (projectRepository.get _).expects(differentProjectId).returning(Option(differentProject).pure[DBResult])
        (taskRepository.get _).expects(taskId).returning(Option(task).pure[DBResult])

        val result = service.update(differentProjectId, taskId, updateInput, ownerId).value.unsafeRunSync()

        result should be(
          ProjectNotMatch(s"task ${taskId.id} not in the project '$differentProjectId'").asLeft[TaskOutput]
        )
      }
      it("return error when task is deleted") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive[IO](taskRepository, projectRepository, readSideService, tnx)

        val now         = Instant.now()
        val deletedTask = task.copy(deletedAt = now.some)

        (projectRepository.get _).expects(projectId).returning(Option(project).pure[DBResult])
        (taskRepository.get _).expects(taskId).returning(Option(deletedTask).pure[DBResult])

        val result = service.update(projectId, taskId, updateInput, ownerId).value.unsafeRunSync()

        result should be(AlreadyDeleted(s"task ${taskId.id} deleted at $now").asLeft[TaskOutput])
      }
    }

    describe("delete should") {
      it("soft task project on write side and update read side") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive[IO](taskRepository, projectRepository, readSideService, tnx)

        val taskDeleted = task.copy(deletedAt = Instant.now.some)

        (projectRepository.get _).expects(projectId).returning(Option(project).pure[DBResult])
        (taskRepository.get _).expects(taskId).returning(Option(task).pure[DBResult])
        (taskRepository.delete _).expects(task).returning(taskDeleted.pure[DBResult])
        (readSideService.taskDeleted _)
          .expects(taskDeleted)
          .returning(Monad[DBResult].unit)

        val result = service.delete(projectId, taskId, ownerId).value.unsafeRunSync()

        result should be(().asRight[AppFailure])
      }
      it("return error when project with given identifier does not exist") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive[IO](taskRepository, projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId).returning(Option(project).pure[DBResult])
        (taskRepository.get _).expects(taskId).returning(Option.empty[TaskEntity].pure[DBResult])

        val result = service.delete(projectId, taskId, ownerId).value.unsafeRunSync()

        result should be(EntityNotFound(s"task ${taskId.id} does not exist").asLeft[Unit])
      }
      it("return error when not owner of the task wants to delete it") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive[IO](taskRepository, projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId).returning(Option(project).pure[DBResult])
        (taskRepository.get _).expects(taskId).returning(Option(task).pure[DBResult])

        val notOwnerId = UserId(UUID.randomUUID())
        val result     = service.delete(projectId, taskId, notOwnerId).value.unsafeRunSync()

        result should be(NotOwner("only owner can delete task").asLeft[Unit])
      }
      it("return error when task is already deleted") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive[IO](taskRepository, projectRepository, readSideService, tnx)

        val deletedAt   = Instant.now
        val taskDeleted = task.copy(deletedAt = deletedAt.some)

        (projectRepository.get _).expects(projectId).returning(Option(project).pure[DBResult])
        (taskRepository.get _).expects(taskId).returning(Option(taskDeleted).pure[DBResult])

        val result = service.delete(projectId, taskId, ownerId).value.unsafeRunSync()

        result should be(AlreadyDeleted(s"task ${taskId.id} deleted at $deletedAt").asLeft[Unit])
      }
      it("when task is not from given project") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new TaskServiceLive[IO](taskRepository, projectRepository, readSideService, tnx)

        val differentProjectId: ProjectId = "p1111"
        val differentProject              = project.copy(dbId = 21L, projectId = differentProjectId)

        (projectRepository.get _).expects(differentProjectId).returning(Option(differentProject).pure[DBResult])
        (taskRepository.get _).expects(taskId).returning(Option(task).pure[DBResult])

        val result = service.delete(differentProjectId, taskId, ownerId).value.unsafeRunSync()

        result should be(ProjectNotMatch(s"task ${taskId.id} not in the project '$differentProjectId'").asLeft[Unit])
      }
    }
  }
}
