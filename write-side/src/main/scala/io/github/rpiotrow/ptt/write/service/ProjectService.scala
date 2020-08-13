package io.github.rpiotrow.ptt.write.service

import cats.data.EitherT
import cats.effect.{Blocker, ContextShift, IO, Resource}
import cats.implicits._
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.{ExecutionContexts, Transactor}
import io.github.rpiotrow.ptt.api.input.ProjectInput
import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}
import io.github.rpiotrow.ptt.api.output.ProjectOutput
import io.github.rpiotrow.ptt.write.configuration.{AppConfiguration, DatabaseConfiguration}
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity}
import io.github.rpiotrow.ptt.write.repository.{DBResult, ProjectReadSideRepository, ProjectRepository}

trait ProjectService {
  def create(input: ProjectInput, owner: UserId): EitherT[IO, NotUnique, ProjectOutput]
  def delete(projectId: ProjectId, user: UserId): EitherT[IO, AppFailure, Unit]
}

object ProjectService {
  def live(implicit contextShift: ContextShift[IO]): IO[ProjectService] = {
    createTransactor(AppConfiguration.live.databaseConfiguration)
      .use(tnx => IO(new ProjectServiceLive(ProjectRepository.live, ProjectReadSideRepository.live, tnx)))
  }

  private def createTransactor(
    configuration: DatabaseConfiguration
  )(implicit contextShift: ContextShift[IO]): Resource[IO, HikariTransactor[IO]] = {

    val hikariConfig = new HikariConfig()
    hikariConfig.setDriverClassName(configuration.jdbcDriver)
    hikariConfig.setJdbcUrl(configuration.jdbcUrl)
    hikariConfig.setUsername(configuration.dbUsername)
    hikariConfig.setPassword(configuration.dbPassword)

    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
      b  <- Blocker[IO]
      t  <- HikariTransactor.fromHikariConfig[IO](hikariConfig, ce, b)
    } yield t
  }

}

private[service] class ProjectServiceLive(
  private val projectRepository: ProjectRepository,
  private val projectReadSideRepository: ProjectReadSideRepository,
  private val tnx: Transactor[IO]
) extends ProjectService {

  override def create(input: ProjectInput, owner: UserId): EitherT[IO, NotUnique, ProjectOutput] = {
    val projectId = input.projectId.value
    (for {
      existingOption <- EitherT.right[NotUnique](projectRepository.get(projectId))
      _              <- checkUniqueness(existingOption)
      entity         <- EitherT.right[NotUnique](projectRepository.create(projectId, owner))
      readSideEntity <- EitherT.right[NotUnique](projectReadSideRepository.newProject(entity))
    } yield toOutput(readSideEntity)).transact(tnx)
  }

  override def delete(projectId: ProjectId, user: UserId): EitherT[IO, AppFailure, Unit] = {
    (for {
      existingOption <- EitherT.right[AppFailure](projectRepository.get(projectId.value))
      project        <- checkExists(existingOption)
      _              <- checkOwner(project, user)
      entity         <- EitherT.right[AppFailure](projectRepository.delete(project))
      _              <- EitherT.right[AppFailure](projectReadSideRepository.deletedProject(entity))
      // TODO: delete all tasks related to project on write and read side
      // TODO: update statistics
    } yield ()).transact(tnx)
  }

  // FIXME: check Uniqueness should take id, read form db and return Unit, this one is more like checkDoesNotExist
  private def checkUniqueness(existingOption: Option[ProjectEntity]): EitherT[DBResult, NotUnique, Unit] = {
    existingOption match {
      case Some(_) =>
        EitherT.left[Unit](NotUnique("project with given projectId already exists").pure[DBResult])
      case None    => EitherT.right[NotUnique](().pure[DBResult])
    }
  }

  private def checkExists(existingOption: Option[ProjectEntity]): EitherT[DBResult, EntityNotFound, ProjectEntity] = {
    existingOption match {
      case Some(entity) => EitherT.right[EntityNotFound](entity.pure[DBResult])
      case None         =>
        EitherT.left[ProjectEntity](EntityNotFound("project with given projectId does not exists").pure[DBResult])
    }
  }

  private def checkOwner(project: ProjectEntity, user: UserId): EitherT[DBResult, NotOwner, Unit] = {
    EitherT.cond[DBResult](project.owner == user, (), NotOwner("only owner can delete project"))
  }

  private def toOutput(readSideEntity: ProjectReadSideEntity) =
    ProjectOutput(
      projectId = readSideEntity.projectId,
      createdAt = readSideEntity.createdAt,
      owner = readSideEntity.owner,
      durationSum = readSideEntity.durationSum,
      tasks = List()
    )
}
