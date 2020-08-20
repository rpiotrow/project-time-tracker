package io.github.rpiotrow.ptt.write.repository

import io.getquill.{idiom => _}
import io.github.rpiotrow.ptt.write.entity.{TaskEntity, TaskReadSideEntity}

trait TaskReadSideRepository {
  def add(task: TaskEntity): DBResult[TaskReadSideEntity]
}

object TaskReadSideRepository {
  val live: TaskReadSideRepository = new TaskReadSideRepositoryLive(liveContext)
}

private[repository] class TaskReadSideRepositoryLive(private val ctx: DBContext) extends TaskReadSideRepository {

  import ctx._

  private val tasksReadSide = quote { querySchema[TaskReadSideEntity]("ptt_read_model.tasks") }

  override def add(task: TaskEntity): DBResult[TaskReadSideEntity] = {
    val readSideEntity = TaskReadSideEntity(task)
    run(quote { tasksReadSide.insert(lift(readSideEntity)).returningGenerated(_.dbId) })
      .map(dbId => readSideEntity.copy(dbId = dbId))
  }

}
