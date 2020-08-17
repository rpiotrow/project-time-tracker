package io.github.rpiotrow.ptt.write.service

import java.sql.Connection
import java.time.{Duration, LocalDateTime}
import java.util.UUID

import cats.implicits._
import cats.effect.{Blocker, ContextShift, IO, Resource}
import doobie.Transactor
import doobie.free.KleisliInterpreter
import doobie.util.ExecutionContexts
import doobie.util.transactor.Strategy
import eu.timepit.refined.auto._
import io.github.rpiotrow.ptt.api.input.{ProjectInput, TaskInput}
import io.github.rpiotrow.ptt.api.model.{ProjectId, TaskId, UserId}
import io.github.rpiotrow.ptt.api.output.{ProjectOutput, TaskOutput}
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity, TaskEntity}
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
  protected val userId: UUID         = UUID.randomUUID()
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

  protected val taskId: TaskId = UUID.randomUUID()
  protected val taskInput      =
    TaskInput(startedAt = now, duration = Duration.ofMinutes(30), volume = 10.some, comment = "text".some)
  protected val taskOutput     = TaskOutput(
    taskId = taskId,
    owner = ownerId,
    startedAt = now,
    duration = Duration.ofMinutes(30),
    volume = 10.some,
    comment = "text".some
  )

  protected val task          = TaskEntity(
    dbId = 11,
    taskId = taskId,
    projectDbId = project.dbId,
    deletedAt = None,
    owner = ownerId,
    startedAt = now,
    duration = Duration.ofMinutes(30),
    volume = 10.some,
    comment = "text".some
  )
  protected val taskReadModel = task

}
