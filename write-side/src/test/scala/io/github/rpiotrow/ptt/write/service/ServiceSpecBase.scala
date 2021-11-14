package io.github.rpiotrow.ptt.write.service

import cats.effect.{IO, Resource}
import cats.implicits._
import doobie.util.transactor.Strategy
import doobie.{KleisliInterpreter, Transactor}
import eu.timepit.refined.auto._
import io.github.rpiotrow.ptt.api.input.{ProjectInput, TaskInput}
import io.github.rpiotrow.ptt.api.model.{ProjectId, TaskId, UserId}
import io.github.rpiotrow.ptt.api.output.{ProjectOutput, TaskOutput}
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity, TaskEntity, TaskReadSideEntity}
import org.scalamock.scalatest.MockFactory

import java.sql.Connection
import java.time._
import java.util.UUID

trait ServiceSpecBase { this: MockFactory =>

  protected val tnx: Transactor[IO] = Transactor(
    (),
    (_: Unit) => Resource.pure[IO, Connection](stub[Connection]),
    KleisliInterpreter[IO].ConnectionInterpreter,
    Strategy.void
  )

  protected val now: Instant                            = Instant.now()
  protected val ownerId: UserId                         = UserId(UUID.randomUUID())
  protected val userId: UserId                          = UserId(UUID.randomUUID())
  protected val projectId: ProjectId                    = "p1"
  protected val projectCreateInput: ProjectInput        = ProjectInput(projectId)
  protected val projectOutput: ProjectOutput            = ProjectOutput(
    projectId = projectId,
    owner = ownerId,
    createdAt = OffsetDateTime.ofInstant(now, ZoneOffset.UTC),
    deletedAt = None,
    durationSum = Duration.ZERO,
    tasks = List()
  )
  protected val project: ProjectEntity                  =
    ProjectEntity(dbId = 1, projectId = projectId, createdAt = now, deletedAt = None, owner = ownerId)
  protected val projectReadModel: ProjectReadSideEntity = ProjectReadSideEntity(
    dbId = 111,
    projectId = projectId,
    createdAt = now,
    lastAddDurationAt = now,
    deletedAt = None,
    owner = ownerId,
    durationSum = Duration.ZERO
  )

  protected val projectIdForUpdate: ProjectId                  = "new-one"
  protected val projectUpdateInput: ProjectInput               = ProjectInput(projectIdForUpdate)
  protected val projectUpdated: ProjectEntity                  = project.copy(projectId = projectIdForUpdate)
  protected val projectUpdatedReadModel: ProjectReadSideEntity = projectReadModel.copy(projectId = projectIdForUpdate)

  protected val taskId: TaskId                       = TaskId.random()
  protected val taskStartedAt: OffsetDateTime        = OffsetDateTime.parse("2020-08-18T17:30:00Z")
  protected val taskStartedAtYearMonth: YearMonth    = YearMonth.of(2020, 8)
  protected val taskDuration: Duration               = Duration.ofMinutes(30)
  protected val taskVolume                           = 10
  protected val taskVolumeWeightedDuration: Duration = Duration.ofMinutes(300)
  protected val taskInput: TaskInput                 =
    TaskInput(startedAt = taskStartedAt, duration = taskDuration, volume = taskVolume.some, comment = "text".some)
  protected val taskOutput: TaskOutput               = TaskOutput(
    taskId = taskId,
    owner = ownerId,
    startedAt = taskStartedAt,
    duration = taskDuration,
    volume = taskVolume.some,
    comment = "text".some,
    deletedAt = None
  )
  protected val task: TaskEntity                     = TaskEntity(
    dbId = 11,
    taskId = taskId,
    projectDbId = project.dbId,
    deletedAt = None,
    owner = ownerId,
    startedAt = taskStartedAt.toInstant,
    createdAt = now,
    duration = taskDuration,
    volume = taskVolume.some,
    comment = "text".some
  )
  protected val taskReadModel: TaskReadSideEntity    = TaskReadSideEntity(task, 111)

  protected val taskIdUpdated: TaskId                    = TaskId.random()
  protected val updateInput: TaskInput                   = taskInput.copy(duration = Duration.ofMinutes(45))
  protected val taskUpdated: TaskEntity                  = task.copy(taskId = taskIdUpdated, duration = Duration.ofMinutes(45))
  protected val taskUpdatedReadModel: TaskReadSideEntity =
    taskReadModel.copy(taskId = taskIdUpdated, duration = Duration.ofMinutes(45))
  protected val taskUpdatedOutput: TaskOutput            =
    taskOutput.copy(taskId = taskIdUpdated, duration = Duration.ofMinutes(45))

}
