package io.github.rpiotrow.ptt.write.service

import cats.Monad
import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import doobie.Transactor
import doobie.implicits._
import io.github.rpiotrow.ptt.api.input.ProjectInput
import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}
import io.github.rpiotrow.ptt.write.entity.ProjectEntity
import io.github.rpiotrow.ptt.write.repository.{DBResult, ProjectRepository, TaskRepository}

trait ProjectService[F[_]] {
  def create(input: ProjectInput, owner: UserId): EitherT[F, AppFailure, Unit]
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

  override def create(input: ProjectInput, owner: UserId): EitherT[F, AppFailure, Unit] = {
    val projectId = input.projectId
    (for {
      _       <- checkUniqueness(input.projectId)
      project <- EitherT.right[AppFailure](projectRepository.create(projectId.value, owner))
      _       <- EitherT.right[AppFailure](readSideService.projectCreated(project))
    } yield ()).transact(tnx)
  }

  override def update(projectId: ProjectId, input: ProjectInput, user: UserId): EitherT[F, AppFailure, Unit] =
    (for {
      projectOption <- EitherT.right[AppFailure](projectRepository.get(projectId.value))
      project       <- ifExists(projectOption, s"project '$projectId' does not exist")
      _             <- checkOwner(project, user, "update")
      _             <- checkUniqueness(input.projectId)
      updated       <- EitherT.right[AppFailure](projectRepository.update(project, input.projectId.value))
      _             <- EitherT.right[AppFailure](readSideService.projectUpdated(projectId, updated))
    } yield ()).transact(tnx)

  override def delete(projectId: ProjectId, user: UserId): EitherT[F, AppFailure, Unit] =
    (for {
      projectOption <- EitherT.right[AppFailure](projectRepository.get(projectId.value))
      project       <- ifExists(projectOption, s"project '$projectId' does not exist")
      _             <- checkOwner(project, user, "delete")
      _             <- checkNotAlreadyDeleted(project)
      deleted       <- EitherT.right[AppFailure](projectRepository.delete(project))
      _             <- EitherT.right[AppFailure](taskRepository.deleteAll(project.dbId, deleted.deletedAt.get))
      _             <- EitherT.right[AppFailure](readSideService.projectDeleted(deleted))
    } yield ()).transact(tnx)

  private def checkUniqueness(projectId: ProjectId): EitherT[DBResult, NotUnique, Unit] =
    for {
      projectOption <- EitherT.right[NotUnique](projectRepository.get(projectId.value))
      result        <- checkIsEmpty(projectOption)
    } yield result

  private def checkIsEmpty(projectOption: Option[ProjectEntity]): EitherT[DBResult, NotUnique, Unit] =
    projectOption match {
      case Some(project) =>
        EitherT.left[Unit](NotUnique(s"project '${project.projectId}' already exists").pure[DBResult])
      case None          =>
        EitherT.right[NotUnique](Monad[DBResult].unit)
    }

  private def checkOwner(project: ProjectEntity, user: UserId, actionName: String): EitherT[DBResult, NotOwner, Unit] =
    EitherT.cond[DBResult](project.owner == user, (), NotOwner(s"only owner can $actionName project"))

  private def checkNotAlreadyDeleted(project: ProjectEntity): EitherT[DBResult, AlreadyDeleted, Unit] =
    EitherT.cond[DBResult](
      project.deletedAt.isEmpty,
      (),
      AlreadyDeleted(s"project '${project.projectId}' deleted at ${project.deletedAt.get}")
    )

}
