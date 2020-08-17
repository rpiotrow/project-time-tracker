package io.github.rpiotrow.ptt.write.repository

import java.time.{Duration, LocalDateTime}
import java.util.UUID

import io.github.rpiotrow.ptt.api.input.TaskInput
import io.github.rpiotrow.ptt.api.model.UserId
import io.github.rpiotrow.ptt.write.entity.TaskEntity

trait TaskRepository {
  def add(projectId: Long, input: TaskInput, owner: UUID): DBResult[TaskEntity]

  def overlapping(userId: UserId, startedAt: LocalDateTime, duration: Duration): DBResult[List[TaskEntity]]
}

object TaskRepository {
  val live: TaskRepository = new TaskRepositoryLive(liveContext)
}

private[repository] class TaskRepositoryLive(private val ctx: DBContext) extends TaskRepository {

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

  override def overlapping(userId: UserId, startedAt: LocalDateTime, duration: Duration): DBResult[List[TaskEntity]] = {
    run(quote {
      tasks
        .filter(_.owner == lift(userId))
        .filter(_.deletedAt.isEmpty)
        .filter(e => (e.startedAt, e.duration) overlaps lift((startedAt, duration)))
    })
  }

}
