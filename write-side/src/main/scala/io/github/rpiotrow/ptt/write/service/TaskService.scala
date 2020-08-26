package io.github.rpiotrow.ptt.write.service

import cats.data.EitherT
import cats.effect.Sync
import doobie.Transactor
import doobie.implicits._
import io.github.rpiotrow.ptt.api.input.TaskInput
import io.github.rpiotrow.ptt.api.model.{ProjectId, TaskId, UserId}
import io.github.rpiotrow.ptt.api.output.TaskOutput
import io.github.rpiotrow.ptt.write.entity.{TaskEntity, TaskReadSideEntity}
import io.github.rpiotrow.ptt.write.repository._

trait TaskService[F[_]] {
  def add(projectId: ProjectId, input: TaskInput, userId: UserId): EitherT[F, AppFailure, TaskOutput]
  def update(taskId: TaskId, input: TaskInput, userId: UserId): EitherT[F, AppFailure, TaskOutput]
  def delete(taskId: TaskId, userId: UserId): EitherT[F, AppFailure, Unit]
}

private[service] class TaskServiceLive[F[_]: Sync](
  private val taskRepository: TaskRepository,
  private val projectRepository: ProjectRepository,
  private val readSideService: ReadSideService,
  private val tnx: Transactor[F]
) extends TaskService[F]
    with ServiceBase {

  override def add(projectId: ProjectId, input: TaskInput, userId: UserId): EitherT[F, AppFailure, TaskOutput] = {
    (for {
      projectOption <- EitherT.right[AppFailure](projectRepository.get(projectId.value))
      project       <- ifExists(projectOption, "project with given identifier does not exist")
      _             <- taskDoesNotOverlap(None, input, userId)
      task          <- EitherT.right[AppFailure](taskRepository.add(project.dbId, input, userId))
      readModel     <- EitherT.right[AppFailure](readSideService.taskAdded(task))
    } yield toOutput(readModel)).transact(tnx)
  }

  override def update(taskId: TaskId, input: TaskInput, userId: UserId): EitherT[F, AppFailure, TaskOutput] = {
    (for {
      taskOption <- EitherT.right[AppFailure](taskRepository.get(taskId))
      task       <- ifExists(taskOption, "task with given identifier does not exist")
      _          <- checkOwner(task, userId)
      _          <- taskDoesNotOverlap(Some(task), input, userId)
      _          <- EitherT.right[AppFailure](taskRepository.delete(task))
      newTask    <- EitherT.right[AppFailure](taskRepository.add(task.projectDbId, input, userId))
      _          <- EitherT.right[AppFailure](readSideService.taskDeleted(task))
      readModel  <- EitherT.right[AppFailure](readSideService.taskAdded(newTask))
    } yield toOutput(readModel)).transact(tnx)
  }

  override def delete(taskId: TaskId, userId: UserId): EitherT[F, AppFailure, Unit] = {
    (for {
      taskOption <- EitherT.right[AppFailure](taskRepository.get(taskId))
      task       <- ifExists(taskOption, "task with given identifier does not exist")
      _          <- checkOwner(task, userId)
      _          <- checkNotDeletedAlready(task)
      deleted    <- EitherT.right[AppFailure](taskRepository.delete(task))
      _          <- EitherT.right[AppFailure](readSideService.taskDeleted(deleted))
    } yield ()).transact(tnx)
  }

  private def taskDoesNotOverlap(
    currentTask: Option[TaskEntity],
    input: TaskInput,
    userId: UserId
  ): EitherT[DBResult, InvalidTimeSpan.type, Unit] = {
    val dbResult = taskRepository.overlapping(userId, input.startedAt, input.duration)
    for {
      list  <- EitherT.right[InvalidTimeSpan.type](dbResult)
      check <-
        EitherT.cond[DBResult](list.isEmpty || list.map(_.dbId) == currentTask.map(_.dbId).toList, (), InvalidTimeSpan)
    } yield check
  }

  private def checkOwner(task: TaskEntity, user: UserId): EitherT[DBResult, NotOwner, Unit] = {
    EitherT.cond[DBResult](task.owner == user, (), NotOwner("only owner can delete task"))
  }

  private def checkNotDeletedAlready(task: TaskEntity): EitherT[DBResult, AlreadyDeleted, Unit] = {
    EitherT
      .cond[DBResult](task.deletedAt.isEmpty, (), AlreadyDeleted(s"task was already deleted at ${task.deletedAt.get}"))
  }

  private def toOutput(task: TaskReadSideEntity): TaskOutput = {
    TaskOutput(
      taskId = task.taskId,
      owner = task.owner,
      startedAt = task.startedAt,
      duration = task.duration,
      volume = task.volume,
      comment = task.comment
    )
  }

}
