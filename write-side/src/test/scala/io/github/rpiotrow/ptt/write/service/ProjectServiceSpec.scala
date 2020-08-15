package io.github.rpiotrow.ptt.write.service

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.softwaremill.diffx.scalatest.DiffMatcher._
import doobie.ConnectionIO
import io.github.rpiotrow.ptt.api.output.ProjectOutput
import io.github.rpiotrow.ptt.write.entity.ProjectEntity
import io.github.rpiotrow.ptt.write.repository.ProjectRepository
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class ProjectServiceSpec extends AnyFunSpec with ServiceSpecBase with MockFactory with should.Matchers {

  describe("create should") {
    it("create new project") {
      val projectRepository = mock[ProjectRepository]
      val readSideService   = mock[ReadSideService]
      val service           = new ProjectServiceLive(projectRepository, readSideService, tnx)

      (projectRepository.get _).expects(projectId.value).returning(Option.empty[ProjectEntity].pure[ConnectionIO])
      (projectRepository.create _).expects(projectId.value, ownerId).returning(project.pure[ConnectionIO])
      (readSideService.projectCreated _)
        .expects(project)
        .returning(EitherT.right[AppFailure](projectReadModel.pure[ConnectionIO]))

      val result = service.create(projectInput, ownerId).value.unsafeRunSync()

      result should matchTo(projectOutput.asRight[AppFailure])
    }
    it("do not create project with the same projectId") {
      val projectRepository = mock[ProjectRepository]
      val readSideService   = mock[ReadSideService]
      val service           = new ProjectServiceLive(projectRepository, readSideService, tnx)

      (projectRepository.get _).expects(projectId.value).returning(Option(project).pure[ConnectionIO])

      val result = service.create(projectInput, ownerId).value.unsafeRunSync()

      result should be(NotUnique("project with given projectId already exists").asLeft[ProjectOutput])
    }
  }
  describe("delete should") {
    it("soft delete project on write and update read side") {
      val projectRepository = mock[ProjectRepository]
      val readSideService   = mock[ReadSideService]
      val service           = new ProjectServiceLive(projectRepository, readSideService, tnx)

      (projectRepository.get _).expects(projectId.value).returning(Option(project).pure[ConnectionIO])
      (projectRepository.delete _).expects(project).returning(project.pure[ConnectionIO])
      // TODO: "delete all tasks of project on the write side"
      (readSideService.projectDeleted _)
        .expects(project)
        .returning(EitherT.right[AppFailure](projectReadModel.pure[ConnectionIO]))

      val result = service.delete(projectId, ownerId).value.unsafeRunSync()

      result should be(().asRight[AppFailure])
    }
    it("return error when project with given id does not exist") {
      val projectRepository = mock[ProjectRepository]
      val readSideService   = mock[ReadSideService]
      val service           = new ProjectServiceLive(projectRepository, readSideService, tnx)

      (projectRepository.get _).expects(projectId.value).returning(Option.empty[ProjectEntity].pure[ConnectionIO])

      val result = service.delete(projectId, ownerId).value.unsafeRunSync()

      result should be(EntityNotFound("project with given projectId does not exists").asLeft[Unit])
    }
    it("return error when not owner of the project wants to delete it") {
      val projectRepository = mock[ProjectRepository]
      val readSideService   = mock[ReadSideService]
      val service           = new ProjectServiceLive(projectRepository, readSideService, tnx)

      (projectRepository.get _).expects(projectId.value).returning(Option(project).pure[ConnectionIO])

      val notOwnerId = UUID.randomUUID()
      val result     = service.delete(projectId, notOwnerId).value.unsafeRunSync()

      result should be(NotOwner("only owner can delete project").asLeft[Unit])
    }
  }

}
