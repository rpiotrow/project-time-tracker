package io.github.rpiotrow.ptt.write.service

import cats.data.EitherT
import cats.effect.IO
import doobie.Transactor
import doobie.implicits._
import io.github.rpiotrow.ptt.api.input.TaskInput
import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}
import io.github.rpiotrow.ptt.api.output.TaskOutput
import io.github.rpiotrow.ptt.write.entity.TaskEntity
import io.github.rpiotrow.ptt.write.repository._
import io.github.rpiotrow.ptt.write.service.ProjectService.ifExists

trait TaskService {
  def add(projectId: ProjectId, taskInput: TaskInput, userId: UserId): EitherT[IO, AppFailure, TaskOutput]
}

private[service] class TaskServiceLive(
  private val taskRepository: TaskRepository,
  private val projectRepository: ProjectRepository,
  private val readSideService: ReadSideService,
  private val tnx: Transactor[IO]
) extends TaskService {

  override def add(projectId: ProjectId, input: TaskInput, userId: UserId): EitherT[IO, AppFailure, TaskOutput] = {
    (for {
      projectOption <- EitherT.right[AppFailure](projectRepository.get(projectId.value))
      project       <- ifExists(projectOption)
      _             <- taskDoesNotOverlap(input, userId)
      task          <- EitherT.right[AppFailure](taskRepository.add(project.dbId, input, userId))
      readModel     <- readSideService.taskAdded(task)
    } yield toOutput(readModel)).transact(tnx)
  }

  private def taskDoesNotOverlap(input: TaskInput, userId: UserId): EitherT[DBResult, InvalidTimeSpan.type, Unit] = {
    val dbResult = taskRepository.overlapping(userId, input.startedAt, input.duration)
    for {
      list  <- EitherT.right[InvalidTimeSpan.type](dbResult)
      check <- EitherT.cond[DBResult](list.isEmpty, (), InvalidTimeSpan)
    } yield check
  }

  private def toOutput(task: TaskEntity): TaskOutput = {
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
