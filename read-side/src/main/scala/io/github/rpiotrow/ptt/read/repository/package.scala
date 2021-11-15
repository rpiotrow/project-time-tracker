package io.github.rpiotrow.ptt.read

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{PostgresZioJdbcContext, SnakeCase}
import io.github.rpiotrow.ptt.read.configuration.DatabaseConfiguration
import zio._
import zio.blocking.Blocking

import java.io.Closeable
import javax.sql.DataSource
import scala.concurrent.ExecutionContext

package object repository {

  type DataSourceCloseable  = DataSource with Closeable
  type RepositoryEnv        = Blocking with Has[DatabaseConfiguration]
  type StatisticsRepository = Has[StatisticsRepository.Service]
  type ProjectRepository    = Has[ProjectRepository.Service]
  type TaskRepository       = Has[TaskRepository.Service]

  type Repositories = StatisticsRepository with ProjectRepository with TaskRepository

  private[repository] val quillContext =
    new PostgresZioJdbcContext(SnakeCase) with CustomDecoders with InstantQuotes

  sealed trait RepositoryFailure
  case class EntityNotFound(id: String)         extends RepositoryFailure
  case class RepositoryThrowable(ex: Throwable) extends RepositoryFailure

  def postgreSQLRepositories(): ZLayer[RepositoryEnv, Throwable, Repositories] =
    datasourceLive() >>> repositoriesLive()

  private def datasourceLive(): ZLayer[RepositoryEnv, Throwable, Has[DataSourceCloseable]] =
    ZLayer.fromManaged(for {
      configuration <- zio.config.getConfig[DatabaseConfiguration].toManaged_
      ds            <- createDatasource(configuration)
    } yield ds)

  private def repositoriesLive(): ZLayer[Has[DataSourceCloseable], Throwable, Repositories] =
    ZLayer.fromServiceMany[DataSourceCloseable, Repositories] { ds => {
      val statistics = StatisticsRepository.live(ds)
      val project    = ProjectRepository.live(ds)
      val task       = TaskRepository.live(ds)
      Has(statistics) ++ Has(project) ++ Has(task)
    }}

  private def createDatasource(configuration: DatabaseConfiguration): Managed[Throwable, DataSourceCloseable] = {
    val hikariConfig = new HikariConfig()
    hikariConfig.setDriverClassName(configuration.jdbcDriver)
    hikariConfig.setJdbcUrl(configuration.jdbcUrl)
    hikariConfig.setUsername(configuration.dbUsername)
    hikariConfig.setPassword(configuration.dbPassword)
    hikariConfig.setSchema(configuration.schema)
    hikariConfig.setReadOnly(true)

    Managed.makeEffect(new HikariDataSource(hikariConfig))(_.close())
  }
}
