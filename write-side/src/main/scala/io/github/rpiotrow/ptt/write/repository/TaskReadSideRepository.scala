package io.github.rpiotrow.ptt.write.repository

import java.time.LocalDateTime

import cats.implicits._
import io.getquill.{idiom => _}
import io.github.rpiotrow.ptt.api.model.TaskId
import io.github.rpiotrow.ptt.write.entity.{TaskEntity, TaskReadSideEntity}

trait TaskReadSideRepository {
  def add(projectDbId: Long, task: TaskEntity): DBResult[TaskReadSideEntity]
  def get(taskId: TaskId): DBResult[Option[TaskReadSideEntity]]
  def getNotDeleted(projectDbId: Long): DBResult[List[TaskReadSideEntity]]
  def delete(dbId: Long, deletedAt: LocalDateTime): DBResult[Unit]
  def deleteAll(projectDbId: Long, deletedAt: LocalDateTime): DBResult[Unit]
}

object TaskReadSideRepository {
  val live: TaskReadSideRepository = new TaskReadSideRepositoryLive(liveContext)
}

private[repository] class TaskReadSideRepositoryLive(private val ctx: DBContext)
    extends TaskReadSideRepository
    with ReadSideRepositoryBase {

  import ctx._

  private val tasksReadSide = quote { querySchema[TaskReadSideEntity]("ptt_read_model.tasks") }

  override def add(projectDbId: Long, task: TaskEntity): DBResult[TaskReadSideEntity] = {
    val readSideEntity = TaskReadSideEntity(task, projectDbId)
    run(quote { tasksReadSide.insert(lift(readSideEntity)).returningGenerated(_.dbId) })
      .map(dbId => readSideEntity.copy(dbId = dbId))
  }

  override def get(taskId: TaskId): DBResult[Option[TaskReadSideEntity]] =
    run(quote {
      tasksReadSide.filter(_.taskId == lift(taskId))
    }).map(_.headOption)

  override def getNotDeleted(projectDbId: Long): DBResult[List[TaskReadSideEntity]] =
    run(quote {
      tasksReadSide.filter(t => t.projectDbId == lift(projectDbId) && t.deletedAt.isEmpty)
    })

  override def delete(dbId: Long, deletedAt: LocalDateTime): DBResult[Unit] =
    run(quote {
      tasksReadSide
        .filter(t => t.dbId == lift(dbId) && t.deletedAt.isEmpty)
        .update(_.deletedAt -> lift(deletedAt.some))
    }).map(logIfNotUpdated(s"no task with id '$dbId'"))

  override def deleteAll(projectDbId: Long, deletedAt: LocalDateTime): DBResult[Unit] =
    run(quote {
      tasksReadSide
        .filter(t => t.projectDbId == lift(projectDbId) && t.deletedAt.isEmpty)
        .update(_.deletedAt -> lift(deletedAt.some))
    }).map(_ => ())

}
