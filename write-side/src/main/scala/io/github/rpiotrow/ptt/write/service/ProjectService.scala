package io.github.rpiotrow.ptt.write.service

import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import doobie.Transactor
import doobie.implicits._
import io.github.rpiotrow.ptt.api.input.ProjectInput
import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}
import io.github.rpiotrow.ptt.api.output.ProjectOutput
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity}
import io.github.rpiotrow.ptt.write.repository.{DBResult, ProjectRepository}

trait ProjectService {
  def create(input: ProjectInput, owner: UserId): EitherT[IO, AppFailure, ProjectOutput]
  def delete(projectId: ProjectId, user: UserId): EitherT[IO, AppFailure, Unit]
}

private[service] class ProjectServiceLive(
  private val projectRepository: ProjectRepository,
  private val readSideService: ReadSideService,
  private val tnx: Transactor[IO]
) extends ProjectService
    with ServiceBase {

  override def create(input: ProjectInput, owner: UserId): EitherT[IO, AppFailure, ProjectOutput] = {
    val projectId = input.projectId.value
    (for {
      existingOption    <- EitherT.right[AppFailure](projectRepository.get(projectId))
      _                 <- checkUniqueness(existingOption)
      project           <- EitherT.right[AppFailure](projectRepository.create(projectId, owner))
      projectSideEntity <- EitherT.right[AppFailure](readSideService.projectCreated(project))
    } yield toOutput(projectSideEntity)).transact(tnx)
  }

  override def delete(projectId: ProjectId, user: UserId): EitherT[IO, AppFailure, Unit] = {
    (for {
      projectOption <- EitherT.right[AppFailure](projectRepository.get(projectId.value))
      project       <- ifExists(projectOption, "project with given projectId does not exists")
      _             <- checkOwner(project, user)
      deleted       <- EitherT.right[AppFailure](projectRepository.delete(project))
      _             <- EitherT.right[AppFailure](readSideService.projectDeleted(deleted))
      // TODO: delete all tasks related to project on the write side
    } yield ()).transact(tnx)
  }

  // FIXME: check Uniqueness should take id, read form db and return Unit, this one is more like checkDoesNotExist
  private def checkUniqueness(existingOption: Option[ProjectEntity]): EitherT[DBResult, NotUnique, Unit] = {
    existingOption match {
      case Some(_) =>
        EitherT.left[Unit](NotUnique("project with given projectId already exists").pure[DBResult])
      case None    =>
        EitherT.right[NotUnique](().pure[DBResult])
    }
  }

  private def checkOwner(project: ProjectEntity, user: UserId): EitherT[DBResult, NotOwner, Unit] = {
    EitherT.cond[DBResult](project.owner == user, (), NotOwner("only owner can delete project"))
  }

  private def toOutput(project: ProjectReadSideEntity) =
    ProjectOutput(
      projectId = project.projectId,
      createdAt = project.createdAt,
      owner = project.owner,
      durationSum = project.durationSum,
      tasks = List()
    )
}
