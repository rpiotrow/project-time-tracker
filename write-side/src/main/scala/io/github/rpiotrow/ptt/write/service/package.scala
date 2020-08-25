package io.github.rpiotrow.ptt.write

import cats.effect.{Async, Blocker, ContextShift, Resource}
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import io.github.rpiotrow.ptt.write.configuration.{AppConfiguration, DatabaseConfiguration}
import io.github.rpiotrow.ptt.write.repository.{ProjectRepository, TaskRepository}

package object service {

  sealed trait AppFailure
  case class EntityNotFound(what: String) extends AppFailure
  case class NotUnique(what: String)      extends AppFailure
  case class NotOwner(what: String)       extends AppFailure
  case object InvalidTimeSpan             extends AppFailure

  def services[F[_]: Async: ContextShift](): Resource[F, (ProjectService[F], TaskService[F])] = {
    for {
      tnx <- createTransactor[F](AppConfiguration.live.databaseConfiguration)
      projectService = new ProjectServiceLive(ProjectRepository.live, TaskRepository.live, ReadSideService.live, tnx)
      taskService    = new TaskServiceLive(TaskRepository.live, ProjectRepository.live, ReadSideService.live, tnx)
    } yield (projectService, taskService)
  }

  private def createTransactor[F[_]: Async: ContextShift](
    configuration: DatabaseConfiguration
  ): Resource[F, HikariTransactor[F]] = {

    val hikariConfig = new HikariConfig()
    hikariConfig.setDriverClassName(configuration.jdbcDriver)
    hikariConfig.setJdbcUrl(configuration.jdbcUrl)
    hikariConfig.setUsername(configuration.dbUsername)
    hikariConfig.setPassword(configuration.dbPassword)

    for {
      ec <- ExecutionContexts.fixedThreadPool[F](32)
      b  <- Blocker[F]
      t  <- HikariTransactor.fromHikariConfig[F](hikariConfig, ec, b)
    } yield t
  }

}
