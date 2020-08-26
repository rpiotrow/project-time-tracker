package io.github.rpiotrow.ptt.write.repository

import java.time.{Clock, Duration, LocalDateTime}
import java.util.UUID

import cats.implicits._
import io.github.rpiotrow.ptt.api.input.TaskInput
import io.github.rpiotrow.ptt.api.model.{TaskId, UserId}
import io.github.rpiotrow.ptt.write.entity.TaskEntity

trait TaskRepository {
  def add(projectId: Long, input: TaskInput, owner: UUID): DBResult[TaskEntity]
  def get(taskId: TaskId): DBResult[Option[TaskEntity]]
  def delete(task: TaskEntity): DBResult[TaskEntity]
  def deleteAll(projectDbId: Long, deletedAt: LocalDateTime): DBResult[Unit]
  def overlapping(userId: UserId, startedAt: LocalDateTime, duration: Duration): DBResult[List[TaskEntity]]
}

object TaskRepository {
  val live: TaskRepository = new TaskRepositoryLive(liveContext)
}

private[repository] class TaskRepositoryLive(private val ctx: DBContext, private val clock: Clock = Clock.systemUTC())
    extends TaskRepository {

  import ctx._

  private val tasks = quote { querySchema[TaskEntity]("ptt.tasks") }

  override def add(projectDbId: Long, input: TaskInput, owner: UserId): DBResult[TaskEntity] = {
    val entity = TaskEntity(
      dbId = 0,
      taskId = UUID.randomUUID(),
      projectDbId = projectDbId,
      deletedAt = None,
      owner = owner,
      startedAt = input.startedAt,
      duration = input.duration,
      volume = input.volume,
      comment = input.comment
    )
    run(quote { tasks.insert(lift(entity)).returningGenerated(_.dbId) })
      .map(dbId => entity.copy(dbId = dbId))
  }

  override def get(taskId: TaskId): DBResult[Option[TaskEntity]] =
    run(quote {
      tasks.filter(_.taskId == lift(taskId))
    }).map(_.headOption)

  override def delete(task: TaskEntity): DBResult[TaskEntity] = {
    val now = LocalDateTime.now(clock)
    run(quote {
      tasks
        .filter(t => t.dbId == lift(task.dbId) && t.deletedAt.isEmpty)
        .update(_.deletedAt -> lift(now.some))
    }).map({
      case 1 => task.copy(deletedAt = now.some)
      case _ => throw new RuntimeException(s"Task '${task.taskId}' not deleted !!!")
    })
  }

  override def deleteAll(projectDbId: Long, deletedAt: LocalDateTime): DBResult[Unit] =
    run(quote {
      tasks
        .filter(t => t.projectDbId == lift(projectDbId) && t.deletedAt.isEmpty)
        .update(_.deletedAt -> lift(deletedAt.some))
    }).map(_ => ())

  override def overlapping(userId: UserId, startedAt: LocalDateTime, duration: Duration): DBResult[List[TaskEntity]] =
    run(quote {
      tasks
        .filter(_.owner == lift(userId))
        .filter(_.deletedAt.isEmpty)
        .filter(e => (e.startedAt, e.duration) overlaps lift((startedAt, duration)))
    })

}
