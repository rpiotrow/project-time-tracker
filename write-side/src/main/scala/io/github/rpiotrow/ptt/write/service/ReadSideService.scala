package io.github.rpiotrow.ptt.write.service

import cats.data.EitherT
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity, TaskEntity}
import io.github.rpiotrow.ptt.write.repository.{DBResult, ProjectReadSideRepository, TaskReadSideRepository}

trait ReadSideService {
  def projectCreated(project: ProjectEntity): EitherT[DBResult, AppFailure, ProjectReadSideEntity]
  def projectDeleted(project: ProjectEntity): EitherT[DBResult, AppFailure, Unit]

  def taskAdded(task: TaskEntity): EitherT[DBResult, AppFailure, TaskEntity]
}

object ReadSideService {
  val live: ReadSideService = new ReadSideServiceLive(ProjectReadSideRepository.live, TaskReadSideRepository.live)
}

private[service] class ReadSideServiceLive(
  private val projectReadSideRepository: ProjectReadSideRepository,
  private val taskReadSideRepository: TaskReadSideRepository
) extends ReadSideService {

  override def projectCreated(project: ProjectEntity): EitherT[DBResult, AppFailure, ProjectReadSideEntity] = {
    EitherT.right[AppFailure](projectReadSideRepository.newProject(project))
  }

  override def projectDeleted(project: ProjectEntity): EitherT[DBResult, AppFailure, Unit] = {
    EitherT.right[AppFailure](projectReadSideRepository.deleteProject(project))
    // TODO: delete all tasks related to project on the read side
    // TODO: update statistics
  }

  override def taskAdded(task: TaskEntity): EitherT[DBResult, AppFailure, TaskEntity] = {
    for {
      readModel <- EitherT.right[AppFailure](taskReadSideRepository.add(task))
      _         <- EitherT.right[AppFailure](projectReadSideRepository.addToProjectDuration(readModel))
      // TODO: update statistics
    } yield readModel
  }

}
