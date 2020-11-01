package io.github.rpiotrow.ptt.write.service

import java.time.Instant
import java.util.UUID

import cats.Monad
import cats.effect.IO
import cats.implicits._
import io.github.rpiotrow.ptt.api.model.UserId
import io.github.rpiotrow.ptt.api.output.ProjectOutput
import io.github.rpiotrow.ptt.write.entity.ProjectEntity
import io.github.rpiotrow.ptt.write.repository.{DBResult, ProjectRepository, TaskRepository}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class ProjectServiceSpec extends AnyFunSpec with ServiceSpecBase with MockFactory with should.Matchers {

  describe("ProjectService") {
    describe("create should") {
      it("create new project") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId).returning(Option.empty[ProjectEntity].pure[DBResult])
        (projectRepository.create _).expects(projectId, ownerId).returning(project.pure[DBResult])
        (readSideService.projectCreated _)
          .expects(project)
          .returning(Monad[DBResult].unit)

        val result = service.create(projectCreateInput, ownerId).value.unsafeRunSync()

        result should be(().asRight[AppFailure])
      }
      it("do not create project with the same projectId") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId).returning(project.some.pure[DBResult])

        val result = service.create(projectCreateInput, ownerId).value.unsafeRunSync()

        result should be(NotUnique(s"project '$projectId' already exists").asLeft[ProjectOutput])
      }
    }

    describe("update should") {
      it("change projectId") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        (projectRepository.get _)
          .expects(projectIdForUpdate)
          .returning(Option.empty[ProjectEntity].pure[DBResult])
        (projectRepository.get _).expects(projectId).returning(project.some.pure[DBResult])
        (projectRepository.update _).expects(project, projectIdForUpdate).returning(projectUpdated.pure[DBResult])
        (readSideService.projectUpdated _)
          .expects(projectId, projectUpdated)
          .returning(Monad[DBResult].unit)

        val result = service.update(projectId, projectUpdateInput, ownerId).value.unsafeRunSync()

        result should be(().asRight[AppFailure])
      }
      it("change projectId for deleted project") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        val now                   = Instant.now()
        val deletedProject        = project.copy(deletedAt = now.some)
        val deletedProjectUpdated = projectUpdated.copy(deletedAt = now.some)

        (projectRepository.get _)
          .expects(projectIdForUpdate)
          .returning(Option.empty[ProjectEntity].pure[DBResult])
        (projectRepository.get _).expects(projectId).returning(deletedProject.some.pure[DBResult])
        (projectRepository.update _)
          .expects(deletedProject, projectIdForUpdate)
          .returning(deletedProjectUpdated.pure[DBResult])
        (readSideService.projectUpdated _)
          .expects(projectId, deletedProjectUpdated)
          .returning(Monad[DBResult].unit)

        val result = service.update(projectId, projectUpdateInput, ownerId).value.unsafeRunSync()

        result should be(().asRight[AppFailure])
      }
      it("return error when project with given id does not exist") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId).returning(Option.empty[ProjectEntity].pure[DBResult])

        val notOwnerId = UserId(UUID.randomUUID())
        val result     = service.update(projectId, projectUpdateInput, notOwnerId).value.unsafeRunSync()

        result should be(EntityNotFound(s"project '$projectId' does not exist").asLeft[Unit])
      }
      it("return error when given not owner wants to update project") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId).returning(project.some.pure[DBResult])

        val notOwnerId = UserId(UUID.randomUUID())
        val result     = service.update(projectId, projectUpdateInput, notOwnerId).value.unsafeRunSync()

        result should be(NotOwner("only owner can update project").asLeft[Unit])
      }
      it("return error when given project id is already used by some project") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId).returning(project.some.pure[DBResult])
        (projectRepository.get _)
          .expects(projectIdForUpdate)
          .returning(project.some.pure[DBResult])

        val result = service.update(projectId, projectUpdateInput, ownerId).value.unsafeRunSync()

        result should be(NotUnique(s"project '$projectId' already exists").asLeft[ProjectOutput])
      }
      it("return failure when repository update is not successful") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        (projectRepository.get _)
          .expects(projectIdForUpdate)
          .returning(Option.empty[ProjectEntity].pure[DBResult])
        (projectRepository.get _).expects(projectId).returning(project.some.pure[DBResult])
        val runtimeException = new RuntimeException("someone.changed.projectId.in.the.mean.time")
        (projectRepository.update _)
          .expects(project, projectIdForUpdate)
          .throwing(runtimeException)

        val result = service.update(projectId, projectUpdateInput, ownerId).value.attempt.unsafeRunSync()

        result should be(runtimeException.asLeft[Either[AppFailure, Unit]])
      }
    }

    describe("delete should") {
      it("soft delete project on write and update read side") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        val deletedProject = project.copy(deletedAt = now.some)

        (projectRepository.get _).expects(projectId).returning(Option(project).pure[DBResult])
        (projectRepository.delete _).expects(project).returning(deletedProject.pure[DBResult])
        (taskRepository.deleteAll _).expects(deletedProject.dbId, now).returning(Monad[DBResult].unit)
        (readSideService.projectDeleted _)
          .expects(deletedProject)
          .returning(().pure[DBResult])

        val result = service.delete(projectId, ownerId).value.unsafeRunSync()

        result should be(().asRight[AppFailure])
      }
      it("return error when project with given id does not exist") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId).returning(Option.empty[ProjectEntity].pure[DBResult])

        val result = service.delete(projectId, ownerId).value.unsafeRunSync()

        result should be(EntityNotFound(s"project '$projectId' does not exist").asLeft[Unit])
      }
      it("return error when not owner of the project wants to delete it") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId).returning(Option(project).pure[DBResult])

        val notOwnerId = UserId(UUID.randomUUID())
        val result     = service.delete(projectId, notOwnerId).value.unsafeRunSync()

        result should be(NotOwner("only owner can delete project").asLeft[Unit])
      }
      it("return error when project is already deleted") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        val deletedAt      = Instant.now
        val deletedProject = project.copy(deletedAt = deletedAt.some)

        (projectRepository.get _).expects(projectId).returning(Option(deletedProject).pure[DBResult])

        val result = service.delete(projectId, ownerId).value.unsafeRunSync()

        result should be(AlreadyDeleted(s"project '$projectId' deleted at $deletedAt").asLeft[Unit])
      }
    }
  }

}
