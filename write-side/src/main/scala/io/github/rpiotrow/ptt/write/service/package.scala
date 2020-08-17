package io.github.rpiotrow.ptt.write

import cats.effect.{Blocker, ContextShift, IO, Resource}
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

  def services(implicit contextShift: ContextShift[IO]): IO[(ProjectService, TaskService)] = {
    createTransactor(AppConfiguration.live.databaseConfiguration)
      .use(tnx => {
        val projectService = new ProjectServiceLive(ProjectRepository.live, ReadSideService.live, tnx)
        val taskService    = new TaskServiceLive(TaskRepository.live, ProjectRepository.live, ReadSideService.live, tnx)
        IO((projectService, taskService))
      })
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
      ec <- ExecutionContexts.fixedThreadPool[IO](32)
      b  <- Blocker[IO]
      t  <- HikariTransactor.fromHikariConfig[IO](hikariConfig, ec, b)
    } yield t
  }

}
