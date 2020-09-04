package io.github.rpiotrow.ptt.write.service

import java.sql.Connection
import java.time.{Duration, LocalDateTime, YearMonth}
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
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity, TaskEntity, TaskReadSideEntity}
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
  protected val ownerId: UserId      = UserId(UUID.randomUUID())
  protected val userId: UserId       = UserId(UUID.randomUUID())
  protected val projectId: ProjectId = "p1"
  protected val projectCreateInput   = ProjectInput(projectId)
  protected val projectOutput        = ProjectOutput(
    projectId = projectId,
    owner = ownerId,
    createdAt = now,
    deletedAt = None,
    durationSum = Duration.ZERO,
    tasks = List()
  )
  protected val project              =
    ProjectEntity(dbId = 1, projectId = projectId, createdAt = now, deletedAt = None, owner = ownerId)
  protected val projectReadModel     = ProjectReadSideEntity(
    dbId = 111,
    projectId = projectId,
    createdAt = now,
    lastAddDurationAt = now,
    deletedAt = None,
    owner = ownerId,
    durationSum = Duration.ZERO
  )

  protected val projectIdForUpdate: ProjectId = "new-one"
  protected val projectUpdateInput            = ProjectInput(projectIdForUpdate)
  protected val projectUpdated                = project.copy(projectId = projectIdForUpdate)
  protected val projectUpdatedReadModel       = projectReadModel.copy(projectId = projectIdForUpdate)

  protected val taskId: TaskId             = TaskId.random()
  protected val taskStartedAt              = LocalDateTime.parse("2020-08-18T17:30:00")
  protected val taskStartedAtYearMonth     = YearMonth.of(2020, 8)
  protected val taskDuration               = Duration.ofMinutes(30)
  protected val taskVolume                 = 10
  protected val taskVolumeWeightedDuration = Duration.ofMinutes(300)
  protected val taskInput                  =
    TaskInput(startedAt = taskStartedAt, duration = taskDuration, volume = taskVolume.some, comment = "text".some)
  protected val taskOutput                 = TaskOutput(
    taskId = taskId,
    owner = ownerId,
    startedAt = taskStartedAt,
    duration = taskDuration,
    volume = taskVolume.some,
    comment = "text".some,
    deletedAt = None
  )
  protected val task                       = TaskEntity(
    dbId = 11,
    taskId = taskId,
    projectDbId = project.dbId,
    deletedAt = None,
    owner = ownerId,
    startedAt = taskStartedAt,
    createdAt = now,
    duration = taskDuration,
    volume = taskVolume.some,
    comment = "text".some
  )
  protected val taskReadModel              = TaskReadSideEntity(task, 111)

  protected val taskIdUpdated        = TaskId.random()
  protected val updateInput          = taskInput.copy(duration = Duration.ofMinutes(45))
  protected val taskUpdated          = task.copy(taskId = taskIdUpdated, duration = Duration.ofMinutes(45))
  protected val taskUpdatedReadModel = taskReadModel.copy(taskId = taskIdUpdated, duration = Duration.ofMinutes(45))
  protected val taskUpdatedOutput    = taskOutput.copy(taskId = taskIdUpdated, duration = Duration.ofMinutes(45))

}
