package io.github.rpiotrow.ptt.write.repository

import io.getquill.{idiom => _}
import io.github.rpiotrow.ptt.write.entity.TaskEntity

trait TaskReadSideRepository {
  def add(task: TaskEntity): DBResult[TaskEntity]
}

object TaskReadSideRepository {
  val live: TaskReadSideRepository = new TaskReadSideRepositoryLive(liveContext)
}

private[repository] class TaskReadSideRepositoryLive(private val ctx: DBContext) extends TaskReadSideRepository {

  import ctx._

  private val tasksReadSide = quote { querySchema[TaskEntity]("ptt_read_model.tasks") }

  override def add(task: TaskEntity): DBResult[TaskEntity] = {
    run(quote { tasksReadSide.insert(lift(task)).returningGenerated(_.dbId) })
      .map(dbId => task.copy(dbId = dbId))
  }

}
