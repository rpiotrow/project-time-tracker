package io.github.rpiotrow.ptt.write.service

import java.sql.Connection
import java.time.{Duration, LocalDateTime}
import java.util.UUID

import cats.effect.{Blocker, ContextShift, IO, Resource}
import doobie.Transactor
import doobie.free.KleisliInterpreter
import doobie.util.ExecutionContexts
import doobie.util.transactor.Strategy
import eu.timepit.refined.auto._
import io.github.rpiotrow.ptt.api.input.ProjectInput
import io.github.rpiotrow.ptt.api.model.ProjectId
import io.github.rpiotrow.ptt.api.output.ProjectOutput
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity}
import org.scalamock.scalatest.MockFactory

trait ServiceSpecBase { this: MockFactory =>

  implicit protected val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)
  protected val blocker                                 = Blocker.liftExecutionContext(ExecutionContexts.synchronous)

  protected val tnx: Transactor[IO] = Transactor(
    (),
    (_: Unit) => Resource.pure[IO, Connection](stub[Connection]),
    KleisliInterpreter[IO](blocker).ConnectionInterpreter,
    Strategy.void
  )

  protected val now                  = LocalDateTime.now()
  protected val ownerId: UUID        = UUID.randomUUID()
  protected val projectId: ProjectId = "p1"
  protected val projectInput         = ProjectInput(projectId)
  protected val projectOutput        = ProjectOutput(
    projectId = projectId.value,
    owner = ownerId,
    createdAt = now,
    durationSum = Duration.ZERO,
    tasks = List()
  )

  protected val project          = ProjectEntity(
    dbId = 1,
    projectId = projectId.value,
    createdAt = now,
    updatedAt = now,
    deletedAt = None,
    owner = ownerId
  )
  protected val projectReadModel = ProjectReadSideEntity(
    dbId = 1,
    projectId = projectId.value,
    createdAt = now,
    updatedAt = now,
    deletedAt = None,
    owner = ownerId,
    durationSum = Duration.ZERO
  )

}
