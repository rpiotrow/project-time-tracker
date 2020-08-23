package io.github.rpiotrow.ptt.write.service

import cats.data.EitherT
import cats.effect.IO
import doobie.Transactor
import doobie.implicits._
import io.github.rpiotrow.ptt.api.input.TaskInput
import io.github.rpiotrow.ptt.api.model.{ProjectId, TaskId, UserId}
import io.github.rpiotrow.ptt.api.output.TaskOutput
import io.github.rpiotrow.ptt.write.entity.{TaskEntity, TaskReadSideEntity}
import io.github.rpiotrow.ptt.write.repository._

trait TaskService {
  def add(projectId: ProjectId, input: TaskInput, userId: UserId): EitherT[IO, AppFailure, TaskOutput]
  def update(taskId: TaskId, input: TaskInput, userId: UserId): EitherT[IO, AppFailure, TaskOutput]
  def delete(taskId: TaskId, userId: UserId): EitherT[IO, AppFailure, Unit]
}

private[service] class TaskServiceLive(
  private val taskRepository: TaskRepository,
  private val projectRepository: ProjectRepository,
  private val readSideService: ReadSideService,
  private val tnx: Transactor[IO]
) extends TaskService
    with ServiceBase {

  override def add(projectId: ProjectId, input: TaskInput, userId: UserId): EitherT[IO, AppFailure, TaskOutput] = {
    (for {
      projectOption <- EitherT.right[AppFailure](projectRepository.get(projectId.value))
      project       <- ifExists(projectOption, "project with given identifier does not exist")
      _             <- taskDoesNotOverlap(None, input, userId)
      task          <- EitherT.right[AppFailure](taskRepository.add(project.dbId, input, userId))
      readModel     <- EitherT.right[AppFailure](readSideService.taskAdded(task))
    } yield toOutput(readModel)).transact(tnx)
  }

  override def update(taskId: TaskId, input: TaskInput, userId: UserId): EitherT[IO, AppFailure, TaskOutput] = {
    (for {
      taskOption <- EitherT.right[AppFailure](taskRepository.get(taskId))
      task       <- ifExists(taskOption, "task with given identifier does not exist")
      _          <- checkOwner(task, userId)
      _          <- taskDoesNotOverlap(Some(task), input, userId)
      _          <- EitherT.right[AppFailure](taskRepository.delete(task))
      _          <- EitherT.right[AppFailure](readSideService.taskDeleted(task))
      newTask    <- EitherT.right[AppFailure](taskRepository.add(task.projectDbId, input, userId))
      readModel  <- EitherT.right[AppFailure](readSideService.taskAdded(newTask))
    } yield toOutput(readModel)).transact(tnx)
  }

  override def delete(taskId: TaskId, userId: UserId): EitherT[IO, AppFailure, Unit] = {
    (for {
      taskOption <- EitherT.right[AppFailure](taskRepository.get(taskId))
      task       <- ifExists(taskOption, "task with given identifier does not exist")
      _          <- checkOwner(task, userId)
      _          <- EitherT.right[AppFailure](taskRepository.delete(task))
      _          <- EitherT.right[AppFailure](readSideService.taskDeleted(task))
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
