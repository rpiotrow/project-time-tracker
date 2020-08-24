package io.github.rpiotrow.ptt.write.service

import cats.Monad
import cats.data.EitherT
import cats.effect.{Async, Bracket, Sync}
import cats.implicits._
import doobie.Transactor
import doobie.implicits._
import io.github.rpiotrow.ptt.api.input.ProjectInput
import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}
import io.github.rpiotrow.ptt.api.output.ProjectOutput
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity}
import io.github.rpiotrow.ptt.write.repository.{DBResult, ProjectRepository, TaskRepository}

trait ProjectService[F[_]] {
  def create(input: ProjectInput, owner: UserId): EitherT[F, AppFailure, ProjectOutput]
  def update(projectId: ProjectId, input: ProjectInput, owner: UserId): EitherT[F, AppFailure, Unit]
  def delete(projectId: ProjectId, user: UserId): EitherT[F, AppFailure, Unit]
}

private[service] class ProjectServiceLive[F[_]: Sync](
  private val projectRepository: ProjectRepository,
  private val taskRepository: TaskRepository,
  private val readSideService: ReadSideService,
  private val tnx: Transactor[F]
) extends ProjectService[F]
    with ServiceBase {

  override def create(input: ProjectInput, owner: UserId): EitherT[F, AppFailure, ProjectOutput] = {
    val projectId = input.projectId.value
    (for {
      existingOption <- EitherT.right[AppFailure](projectRepository.get(projectId))
      _              <- checkUniqueness(existingOption)
      project        <- EitherT.right[AppFailure](projectRepository.create(projectId, owner))
      readModel      <- EitherT.right[AppFailure](readSideService.projectCreated(project))
    } yield toOutput(readModel)).transact(tnx)
  }

  override def update(projectId: ProjectId, input: ProjectInput, user: UserId): EitherT[F, AppFailure, Unit] = {
    (for {
      projectOption  <- EitherT.right[AppFailure](projectRepository.get(projectId.value))
      project        <- ifExists(projectOption, "project with given projectId does not exists")
      _              <- checkOwner(project, user, "update")
      existingOption <- EitherT.right[AppFailure](projectRepository.get(input.projectId.value))
      _              <- checkUniqueness(existingOption)
      updated        <- EitherT.right[AppFailure](projectRepository.update(project, input.projectId.value))
      _              <- EitherT.right[AppFailure](readSideService.projectUpdated(projectId, updated))
    } yield ()).transact(tnx)
  }

  override def delete(projectId: ProjectId, user: UserId): EitherT[F, AppFailure, Unit] = {
    (for {
      projectOption <- EitherT.right[AppFailure](projectRepository.get(projectId.value))
      project       <- ifExists(projectOption, "project with given projectId does not exists")
      _             <- checkOwner(project, user, "delete")
      deleted       <- EitherT.right[AppFailure](projectRepository.delete(project))
      _             <- EitherT.right[AppFailure](taskRepository.deleteAll(project.dbId, deleted.deletedAt.get))
      _             <- EitherT.right[AppFailure](readSideService.projectDeleted(deleted))
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

  private def checkOwner(
    project: ProjectEntity,
    user: UserId,
    actionName: String
  ): EitherT[DBResult, NotOwner, Unit] = {
    EitherT.cond[DBResult](project.owner == user, (), NotOwner(s"only owner can $actionName project"))
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
