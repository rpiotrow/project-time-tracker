package io.github.rpiotrow.ptt.write.entity

import java.time.{Duration, LocalDateTime}

import io.github.rpiotrow.ptt.api.model.{TaskId, UserId}

case class TaskReadSideEntity(
  dbId: Long,
  taskId: TaskId,
  projectDbId: Long,
  deletedAt: Option[LocalDateTime],
  owner: UserId,
  startedAt: LocalDateTime,
  duration: Duration,
  volume: Option[Int],
  comment: Option[String]
)

object TaskReadSideEntity {
  def apply(taskEntity: TaskEntity): TaskReadSideEntity = {
    import shapeless._, ops.hlist.Align
    val taskGen         = LabelledGeneric[TaskEntity]
    val taskReadSideGen = LabelledGeneric[TaskReadSideEntity]
    val align           = Align[taskGen.Repr, taskReadSideGen.Repr]
    taskReadSideGen.from(align(taskGen.to(taskEntity)))
  }
}
