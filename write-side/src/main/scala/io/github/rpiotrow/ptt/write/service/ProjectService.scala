package io.github.rpiotrow.ptt.write.service

import cats.data.EitherT
import cats.effect.{Blocker, ContextShift, IO, Resource, Sync}
import cats.implicits._
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.{ExecutionContexts, Transactor}
import io.github.rpiotrow.ptt.api.input.ProjectInput
import io.github.rpiotrow.ptt.api.model.UserId
import io.github.rpiotrow.ptt.api.output.ProjectOutput
import io.github.rpiotrow.ptt.write.configuration.{AppConfiguration, DatabaseConfiguration}
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity}
import io.github.rpiotrow.ptt.write.repository.{DBResult, ProjectReadSideRepository, ProjectRepository}

trait ProjectService {
  def create(input: ProjectInput, owner: UserId): EitherT[IO, NotUnique, ProjectOutput]
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
    (for {
      existingOption <- EitherT.right[NotUnique](projectRepository.get(input.projectId.value))
      _              <- checkUniqueness(existingOption)
      entity         <- EitherT.right[NotUnique](projectRepository.create(input.projectId.value, owner))
      readSideEntity <- EitherT.right[NotUnique](projectReadSideRepository.newProject(entity))
    } yield toOutput(readSideEntity)).transact(tnx)
  }

  private def checkUniqueness(existingOption: Option[ProjectEntity]): EitherT[DBResult, NotUnique, Unit] = {
    existingOption match {
      case Some(_) =>
        EitherT.left[Unit](NotUnique("project with given projectId already exists").pure[DBResult])
      case None    => EitherT.right[NotUnique](().pure[DBResult])
    }
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
