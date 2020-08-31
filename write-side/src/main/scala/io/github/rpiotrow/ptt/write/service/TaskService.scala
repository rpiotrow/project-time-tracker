package io.github.rpiotrow.ptt.write.service

import cats.data.EitherT
import cats.effect.Sync
import doobie.Transactor
import doobie.implicits._
import io.github.rpiotrow.ptt.api.input.TaskInput
import io.github.rpiotrow.ptt.api.model.{ProjectId, TaskId, UserId}
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, TaskEntity}
import io.github.rpiotrow.ptt.write.repository._

trait TaskService[F[_]] {
  def add(projectId: ProjectId, input: TaskInput, userId: UserId): EitherT[F, AppFailure, TaskId]
  def update(projectId: ProjectId, taskId: TaskId, input: TaskInput, userId: UserId): EitherT[F, AppFailure, TaskId]
  def delete(projectId: ProjectId, taskId: TaskId, userId: UserId): EitherT[F, AppFailure, Unit]
}

private[service] class TaskServiceLive[F[_]: Sync](
  private val taskRepository: TaskRepository,
  private val projectRepository: ProjectRepository,
  private val readSideService: ReadSideService,
  private val tnx: Transactor[F]
) extends TaskService[F]
    with ServiceBase {

  override def add(projectId: ProjectId, input: TaskInput, userId: UserId): EitherT[F, AppFailure, TaskId] = {
    (for {
      projectOption <- EitherT.right[AppFailure](projectRepository.get(projectId.value))
      project       <- ifExists(projectOption, "project with given identifier does not exist")
      _             <- taskDoesNotOverlap(None, input, userId)
      task          <- EitherT.right[AppFailure](taskRepository.add(project.dbId, input, userId))
      _             <- EitherT.right[AppFailure](readSideService.taskAdded(projectId.value, task))
    } yield task.taskId).transact(tnx)
  }

  override def update(
    projectId: ProjectId,
    taskId: TaskId,
    input: TaskInput,
    userId: UserId
  ): EitherT[F, AppFailure, TaskId] = {
    (for {
      projectOption <- EitherT.right[AppFailure](projectRepository.get(projectId.value))
      project       <- ifExists(projectOption, "project with given identifier does not exist")
      taskOption    <- EitherT.right[AppFailure](taskRepository.get(taskId))
      task          <- ifExists(taskOption, "task with given identifier does not exist")
      _             <- checkOwner(task, userId)
      _             <- checkProject(task, project)
      _             <- taskDoesNotOverlap(Some(task), input, userId)
      _             <- EitherT.right[AppFailure](taskRepository.delete(task))
      newTask       <- EitherT.right[AppFailure](taskRepository.add(task.projectDbId, input, userId))
      _             <- EitherT.right[AppFailure](readSideService.taskDeleted(task))
      _             <- EitherT.right[AppFailure](readSideService.taskAdded(project.projectId, newTask))
    } yield newTask.taskId).transact(tnx)
  }

  override def delete(projectId: ProjectId, taskId: TaskId, userId: UserId): EitherT[F, AppFailure, Unit] = {
    (for {
      projectOption <- EitherT.right[AppFailure](projectRepository.get(projectId.value))
      project       <- ifExists(projectOption, "project with given identifier does not exist")
      taskOption    <- EitherT.right[AppFailure](taskRepository.get(taskId))
      task          <- ifExists(taskOption, "task with given identifier does not exist")
      _             <- checkOwner(task, userId)
      _             <- checkProject(task, project)
      _             <- checkNotDeletedAlready(task)
      deleted       <- EitherT.right[AppFailure](taskRepository.delete(task))
      _             <- EitherT.right[AppFailure](readSideService.taskDeleted(deleted))
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

  private def checkProject(task: TaskEntity, project: ProjectEntity): EitherT[DBResult, ProjectNotMatch, Unit] = {
    EitherT.cond[DBResult](
      task.projectDbId == project.dbId,
      (),
      ProjectNotMatch("task not in the project with given project identifier")
    )
  }

  private def checkNotDeletedAlready(task: TaskEntity): EitherT[DBResult, AlreadyDeleted, Unit] = {
    EitherT
      .cond[DBResult](task.deletedAt.isEmpty, (), AlreadyDeleted(s"task was already deleted at ${task.deletedAt.get}"))
  }

}
