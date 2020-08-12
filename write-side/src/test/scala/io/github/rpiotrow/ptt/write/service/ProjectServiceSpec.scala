package io.github.rpiotrow.ptt.write.service

import java.sql.Connection
import java.time.{Duration, LocalDateTime}
import java.util.UUID

import cats.effect.{Blocker, ContextShift, IO, Resource}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should
import com.softwaremill.diffx.scalatest.DiffMatcher._
import doobie.{ConnectionIO, Transactor}
import io.github.rpiotrow.ptt.api.input.ProjectInput
import io.github.rpiotrow.ptt.write.repository.{ProjectReadSideRepository, ProjectRepository}
import eu.timepit.refined.auto._
import io.github.rpiotrow.ptt.api.output.ProjectOutput
import cats.implicits._
import doobie.free.KleisliInterpreter
import doobie.util.ExecutionContexts
import doobie.util.transactor.Strategy
import io.github.rpiotrow.ptt.api.model.ProjectId
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity}

class ProjectServiceSpec extends AnyFunSpec with MockFactory with should.Matchers {

  implicit private val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)
  private val blocker                                 = Blocker.liftExecutionContext(ExecutionContexts.synchronous)

  private val tnx: Transactor[IO] = Transactor(
    (),
    (_: Unit) => Resource.pure[IO, Connection](stub[Connection]),
    KleisliInterpreter[IO](blocker).ConnectionInterpreter,
    Strategy.void
  )

  describe("create project should") {
    it("create new project") {
      val projectRepository         = mock[ProjectRepository]
      val projectReadSideRepository = mock[ProjectReadSideRepository]
      val service                   = new ProjectServiceLive(projectRepository, projectReadSideRepository, tnx)

      (projectRepository.get _).expects(projectId.value).returning(Option.empty[ProjectEntity].pure[ConnectionIO])
      (projectRepository.create _).expects(projectId.value, ownerId).returning(entity.pure[ConnectionIO])
      (projectReadSideRepository.newProject _).expects(entity).returning(readSideEntity.pure[ConnectionIO])

      val result = service.create(projectInput, ownerId).value.unsafeRunSync()

      result should matchTo(projectOutput.asRight[NotUnique])
    }
    it("do not create project with the same projectId") {
      val projectRepository         = mock[ProjectRepository]
      val projectReadSideRepository = mock[ProjectReadSideRepository]
      val service                   = new ProjectServiceLive(projectRepository, projectReadSideRepository, tnx)

      (projectRepository.get _).expects(projectId.value).returning(Option(entity).pure[ConnectionIO])

      val result = service.create(projectInput, ownerId).value.unsafeRunSync()

      result should be(NotUnique("project with given projectId already exists").asLeft[ProjectOutput])
    }
  }

  private val now                  = LocalDateTime.now()
  private val ownerId: UUID        = UUID.randomUUID()
  private val projectId: ProjectId = "p1"
  private val projectInput         = ProjectInput(projectId)
  private val projectOutput        =
    ProjectOutput(
      projectId = projectId.value,
      owner = ownerId,
      createdAt = now,
      durationSum = Duration.ZERO,
      tasks = List()
    )

  private val entity         =
    ProjectEntity(
      dbId = 1,
      projectId = projectId.value,
      createdAt = now,
      updatedAt = now,
      deletedAt = None,
      owner = ownerId
    )
  private val readSideEntity = ProjectReadSideEntity(
    dbId = 1,
    projectId = projectId.value,
    createdAt = now,
    updatedAt = now,
    deletedAt = None,
    owner = ownerId,
    durationSum = Duration.ZERO
  )

}
