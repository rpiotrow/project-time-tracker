package io.github.rpiotrow.ptt.write.service

import java.time.LocalDateTime
import java.util.UUID

import cats.Monad
import cats.effect.IO
import cats.implicits._
import com.softwaremill.diffx.scalatest.DiffMatcher._
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

        (projectRepository.get _).expects(projectId.value).returning(Option.empty[ProjectEntity].pure[DBResult])
        (projectRepository.create _).expects(projectId.value, ownerId).returning(project.pure[DBResult])
        (readSideService.projectCreated _)
          .expects(project)
          .returning(projectReadModel.pure[DBResult])

        val result = service.create(projectCreateInput, ownerId).value.unsafeRunSync()

        result should matchTo(projectOutput.asRight[AppFailure])
      }
      it("do not create project with the same projectId") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId.value).returning(project.some.pure[DBResult])

        val result = service.create(projectCreateInput, ownerId).value.unsafeRunSync()

        result should be(NotUnique("project with given projectId already exists").asLeft[ProjectOutput])
      }
    }

    describe("update should") {
      it("change projectId") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        (projectRepository.get _)
          .expects(projectIdForUpdate.value)
          .returning(Option.empty[ProjectEntity].pure[DBResult])
        (projectRepository.get _).expects(projectId.value).returning(project.some.pure[DBResult])
        (projectRepository.update _).expects(project, projectIdForUpdate.value).returning(projectUpdated.pure[DBResult])
        (readSideService.projectUpdated _)
          .expects(projectId, projectUpdated)
          .returning(Monad[DBResult].unit)

        val result = service.update(projectId, projectUpdateInput, ownerId).value.unsafeRunSync()

        result should be(().asRight[AppFailure])
      }
      it("return error when project with given id does not exist") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId.value).returning(Option.empty[ProjectEntity].pure[DBResult])

        val notOwnerId = UUID.randomUUID()
        val result     = service.update(projectId, projectUpdateInput, notOwnerId).value.unsafeRunSync()

        result should be(EntityNotFound("project with given projectId does not exists").asLeft[Unit])
      }
      it("return error when given not owner wants to update project") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId.value).returning(project.some.pure[DBResult])

        val notOwnerId = UUID.randomUUID()
        val result     = service.update(projectId, projectUpdateInput, notOwnerId).value.unsafeRunSync()

        result should be(NotOwner("only owner can update project").asLeft[Unit])
      }
      it("return error when given project id is already used by some project") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId.value).returning(project.some.pure[DBResult])
        (projectRepository.get _)
          .expects(projectIdForUpdate.value)
          .returning(project.some.pure[DBResult])

        val result = service.update(projectId, projectUpdateInput, ownerId).value.unsafeRunSync()

        result should be(NotUnique("project with given projectId already exists").asLeft[ProjectOutput])
      }
      it("return failure when repository update is not successful") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        (projectRepository.get _)
          .expects(projectIdForUpdate.value)
          .returning(Option.empty[ProjectEntity].pure[DBResult])
        (projectRepository.get _).expects(projectId.value).returning(project.some.pure[DBResult])
        val runtimeException = new RuntimeException("someone.changed.projectId.in.the.mean.time")
        (projectRepository.update _)
          .expects(project, projectIdForUpdate.value)
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

        (projectRepository.get _).expects(projectId.value).returning(Option(project).pure[DBResult])
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

        (projectRepository.get _).expects(projectId.value).returning(Option.empty[ProjectEntity].pure[DBResult])

        val result = service.delete(projectId, ownerId).value.unsafeRunSync()

        result should be(EntityNotFound("project with given projectId does not exists").asLeft[Unit])
      }
      it("return error when not owner of the project wants to delete it") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId.value).returning(Option(project).pure[DBResult])

        val notOwnerId = UUID.randomUUID()
        val result     = service.delete(projectId, notOwnerId).value.unsafeRunSync()

        result should be(NotOwner("only owner can delete project").asLeft[Unit])
      }
      it("return error when project is already deleted") {
        val projectRepository = mock[ProjectRepository]
        val taskRepository    = mock[TaskRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive[IO](projectRepository, taskRepository, readSideService, tnx)

        val deletedAt      = LocalDateTime.now
        val deletedProject = project.copy(deletedAt = deletedAt.some)

        (projectRepository.get _).expects(projectId.value).returning(Option(deletedProject).pure[DBResult])

        val result = service.delete(projectId, ownerId).value.unsafeRunSync()

        result should be(AlreadyDeleted(s"project was already deleted at $deletedAt").asLeft[Unit])
      }
    }
  }

}
