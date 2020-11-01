package io.github.rpiotrow.ptt.read.service

import java.time.{Instant, ZoneOffset, OffsetDateTime}

import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}
import io.github.rpiotrow.ptt.api.output.{ProjectOutput, TaskOutput}
import io.github.rpiotrow.ptt.api.param.ProjectListParams
import io.github.rpiotrow.ptt.read.entity.{ProjectEntity, TaskEntity}
import io.github.rpiotrow.ptt.read.repository.{
  ProjectRepository,
  RepositoryFailure,
  RepositoryThrowable,
  TaskRepository
}
import zio.{IO, Task}

object ProjectService {
  trait Service {
    def one(id: ProjectId): IO[RepositoryFailure, ProjectOutput]
    def list(params: ProjectListParams): IO[RepositoryThrowable, List[ProjectOutput]]
  }
  def live(projectRepository: ProjectRepository.Service, taskRepository: TaskRepository.Service): Service =
    new ProjectServiceLive(projectRepository, taskRepository)
}

private class ProjectServiceLive(
  private val projectRepository: ProjectRepository.Service,
  private val taskRepository: TaskRepository.Service
) extends ProjectService.Service {

  override def one(id: ProjectId): IO[RepositoryFailure, ProjectOutput] = {
    for {
      project <- projectRepository.one(id)
      tasks   <- taskRepository.read(List(project.dbId))
    } yield toOutput(project, tasks)
  }

  override def list(params: ProjectListParams): IO[RepositoryThrowable, List[ProjectOutput]] = {
    for {
      projects <- projectRepository.list(params)
      ids = projects.map(_.dbId)
      tasks <- taskRepository.read(ids)
      grouped = tasks.groupBy(_.projectDbId)
      outputs = projects.map(project => toOutput(project, grouped.get(project.dbId).toList.flatten))
    } yield outputs
  }

  private def toOutput(task: TaskEntity): TaskOutput =
    TaskOutput(
      taskId = task.taskId,
      owner = task.owner,
      startedAt = toOffsetDateTime(task.startedAt),
      duration = task.duration,
      volume = task.volume,
      comment = task.comment,
      deletedAt = task.deletedAt.map(toOffsetDateTime)
    )

  private def toOutput(project: ProjectEntity, tasks: List[TaskEntity]): ProjectOutput =
    ProjectOutput(
      projectId = project.projectId,
      createdAt = toOffsetDateTime(project.createdAt),
      deletedAt = project.deletedAt.map(toOffsetDateTime),
      owner = project.owner,
      durationSum = project.durationSum,
      tasks = tasks.map(toOutput)
    )

  private def toOffsetDateTime(instant: Instant) = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC)
}
