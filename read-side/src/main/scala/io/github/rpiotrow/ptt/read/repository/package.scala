package io.github.rpiotrow.ptt.read

import java.util.UUID

import cats.effect.Blocker
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import io.getquill.{idiom => _}
import io.github.rpiotrow.ptt.read.configuration.DatabaseConfiguration
import io.github.rpiotrow.ptt.read.entity._
import zio._
import zio.blocking.Blocking
import zio.config.Config
import zio.interop.catz._

import scala.concurrent.ExecutionContext

package object repository {

  type RepositoryEnv        = Blocking with Config[DatabaseConfiguration]
  type StatisticsRepository = Has[StatisticsRepository.Service]
  type ProjectRepository    = Has[ProjectRepository.Service]
  type TaskRepository       = Has[TaskRepository.Service]

  type Repositories = StatisticsRepository with ProjectRepository with TaskRepository

  def read(owners: List[UUID], range: YearMonthRange): ZIO[StatisticsRepository, Throwable, List[StatisticsEntity]] =
    ZIO.accessM(_.get.read(owners, range))

  def postgreSQL(connectEC: ExecutionContext): ZLayer[RepositoryEnv, Throwable, Repositories] =
    transactorLive(connectEC) >>> repositoriesLive()

  private def transactorLive(
    connectEC: ExecutionContext
  ): ZLayer[RepositoryEnv, Throwable, Has[HikariTransactor[Task]]] =
    ZLayer.fromManaged(for {
      blockingEC    <- blocking.blocking { ZIO.descriptor.map(_.executor.asEC) }.toManaged_
      configuration <- zio.config.config[DatabaseConfiguration].toManaged_
      tnx           <- createTransactor(configuration, connectEC, blockingEC)
    } yield tnx)

  private def repositoriesLive(): ZLayer[Has[HikariTransactor[Task]], Throwable, Repositories] =
    ZLayer.fromServiceMany[HikariTransactor[Task], Repositories](tnx => {
      val statistics = StatisticsRepository.live(tnx)
      val project    = ProjectRepository.live(tnx)
      val task       = TaskRepository.live(tnx)
      Has(statistics) ++ Has(project) ++ Has(task)
    })

  private def createTransactor(
    configuration: DatabaseConfiguration,
    connectEC: ExecutionContext,
    transactEC: ExecutionContext
  ): Managed[Throwable, HikariTransactor[Task]] = {

    val hikariConfig = new HikariConfig()
    hikariConfig.setDriverClassName(configuration.jdbcDriver)
    hikariConfig.setJdbcUrl(configuration.jdbcUrl)
    hikariConfig.setUsername(configuration.dbUsername)
    hikariConfig.setPassword(configuration.dbPassword)
    hikariConfig.setSchema(configuration.schema)

    HikariTransactor
      .fromHikariConfig[Task](hikariConfig, connectEC, Blocker.liftExecutionContext(transactEC))
      .toManagedZIO
  }

}
