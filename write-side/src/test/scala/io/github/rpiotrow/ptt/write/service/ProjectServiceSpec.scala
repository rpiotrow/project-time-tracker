package io.github.rpiotrow.ptt.write.service

import java.util.UUID

import cats.Monad
import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import com.softwaremill.diffx.scalatest.DiffMatcher._
import io.github.rpiotrow.ptt.api.output.ProjectOutput
import io.github.rpiotrow.ptt.write.entity.ProjectEntity
import io.github.rpiotrow.ptt.write.repository.{DBResult, ProjectRepository}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class ProjectServiceSpec extends AnyFunSpec with ServiceSpecBase with MockFactory with should.Matchers {

  describe("ProjectService") {
    describe("create should") {
      it("create new project") {
        val projectRepository = mock[ProjectRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive(projectRepository, readSideService, tnx)

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
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive(projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId.value).returning(project.some.pure[DBResult])

        val result = service.create(projectCreateInput, ownerId).value.unsafeRunSync()

        result should be(NotUnique("project with given projectId already exists").asLeft[ProjectOutput])
      }
    }

    describe("update should") {
      it("change projectId") {
        val projectRepository = mock[ProjectRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive(projectRepository, readSideService, tnx)

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
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive(projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId.value).returning(Option.empty[ProjectEntity].pure[DBResult])

        val notOwnerId = UUID.randomUUID()
        val result     = service.update(projectId, projectUpdateInput, notOwnerId).value.unsafeRunSync()

        result should be(EntityNotFound("project with given projectId does not exists").asLeft[Unit])
      }
      it("return error when given not owner wants to update project") {
        val projectRepository = mock[ProjectRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive(projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId.value).returning(project.some.pure[DBResult])

        val notOwnerId = UUID.randomUUID()
        val result     = service.update(projectId, projectUpdateInput, notOwnerId).value.unsafeRunSync()

        result should be(NotOwner("only owner can update project").asLeft[Unit])
      }
      it("return error when given project id is already used by some project") {
        val projectRepository = mock[ProjectRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive(projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId.value).returning(project.some.pure[DBResult])
        (projectRepository.get _)
          .expects(projectIdForUpdate.value)
          .returning(project.some.pure[DBResult])

        val result = service.update(projectId, projectUpdateInput, ownerId).value.unsafeRunSync()

        result should be(NotUnique("project with given projectId already exists").asLeft[ProjectOutput])
      }
      it("return failure when repository update is not successful") {
        val projectRepository = mock[ProjectRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive(projectRepository, readSideService, tnx)

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
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive(projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId.value).returning(Option(project).pure[DBResult])
        (projectRepository.delete _).expects(project).returning(project.pure[DBResult])
        // TODO: "delete all tasks of project on the write side"
        (readSideService.projectDeleted _)
          .expects(project)
          .returning(().pure[DBResult])

        val result = service.delete(projectId, ownerId).value.unsafeRunSync()

        result should be(().asRight[AppFailure])
      }
      it("return error when project with given id does not exist") {
        val projectRepository = mock[ProjectRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive(projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId.value).returning(Option.empty[ProjectEntity].pure[DBResult])

        val result = service.delete(projectId, ownerId).value.unsafeRunSync()

        result should be(EntityNotFound("project with given projectId does not exists").asLeft[Unit])
      }
      it("return error when not owner of the project wants to delete it") {
        val projectRepository = mock[ProjectRepository]
        val readSideService   = mock[ReadSideService]
        val service           = new ProjectServiceLive(projectRepository, readSideService, tnx)

        (projectRepository.get _).expects(projectId.value).returning(Option(project).pure[DBResult])

        val notOwnerId = UUID.randomUUID()
        val result     = service.delete(projectId, notOwnerId).value.unsafeRunSync()

        result should be(NotOwner("only owner can delete project").asLeft[Unit])
      }
    }
  }

}
