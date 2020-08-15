package io.github.rpiotrow.ptt.write.service

import cats.data.EitherT
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity}
import io.github.rpiotrow.ptt.write.repository.{DBResult, ProjectReadSideRepository}

trait ReadSideService {
  def projectCreated(project: ProjectEntity): EitherT[DBResult, AppFailure, ProjectReadSideEntity]
  def projectDeleted(project: ProjectEntity): EitherT[DBResult, AppFailure, ProjectReadSideEntity]
}

object ReadSideService {
  val live: ReadSideService = new ReadSideServiceLive(ProjectReadSideRepository.live)
}

private[service] class ReadSideServiceLive(private val projectReadSideRepository: ProjectReadSideRepository)
    extends ReadSideService {

  override def projectCreated(project: ProjectEntity): EitherT[DBResult, AppFailure, ProjectReadSideEntity] = {
    EitherT.right[AppFailure](projectReadSideRepository.newProject(project))
  }

  override def projectDeleted(project: ProjectEntity): EitherT[DBResult, AppFailure, ProjectReadSideEntity] = {
    EitherT.right[AppFailure](projectReadSideRepository.deleteProject(project))
    // TODO: delete all tasks related to project on write and read side
    // TODO: update statistics
  }

}
