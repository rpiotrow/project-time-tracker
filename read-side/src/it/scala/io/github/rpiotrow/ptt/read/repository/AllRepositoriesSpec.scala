package io.github.rpiotrow.ptt.read.repository

import java.util.UUID

import cats.effect.Blocker
import com.dimafeng.testcontainers.{ForAllTestContainer, JdbcDatabaseContainer, PostgreSQLContainer}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.Transactor
import doobie.implicits._
import doobie.util.ExecutionContexts
import doobie.util.update.Update0
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers._
import zio._
import zio.interop.catz._

import scala.concurrent.duration.DurationInt
import scala.io.Source

class AllRepositoriesSpec
    extends AnyFunSpec
    with ForAllTestContainer
    with should.Matchers
    with StatisticsRepositorySpec
    with ProjectRepositorySpec
    with TaskRepositorySpec {

  override val container = new PostgreSQLContainer(
    commonJdbcParams = JdbcDatabaseContainer.CommonParams(startupTimeout = 240.seconds, connectTimeout = 240.seconds)
  )

  override val owner1Id = UUID.fromString("41a854e4-4262-4672-a7df-c781f535d6ee")
  override val owner2Id = UUID.fromString("66ffc00e-083b-48aa-abb5-8ef46ac0e06e")

  private lazy val hikariConfig = {
    val hc = new HikariConfig()
    hc.setDriverClassName("org.postgresql.Driver")
    hc.setJdbcUrl(container.jdbcUrl)
    hc.setUsername("reader")
    hc.setPassword("reader")
    hc.setSchema("ptt_read_model")
    hc
  }
  private lazy val ds              = new HikariDataSource(hikariConfig)
  private lazy val tnx             = Transactor.fromDataSource[Task](
    ds,
    ExecutionContexts.synchronous,
    Blocker.liftExecutionContext(ExecutionContexts.synchronous)
  )
  override lazy val projectRepo    = ProjectRepository.live(tnx)
  override lazy val taskRepo       = TaskRepository.live(tnx)
  override lazy val statisticsRepo = StatisticsRepository.live(tnx)

  override def afterStart(): Unit = {
    val tnx  = Transactor.fromDriverManager[Task](
      "org.postgresql.Driver",
      container.jdbcUrl,
      container.username,
      container.password,
      Blocker.liftExecutionContext(ExecutionContexts.synchronous)
    )
    val task = ZManaged
      .make(IO(Source.fromFile("local-dev/create-schema.sql")))(s => IO(s.close()).orDie)
      .use(
        createSchemaSQL =>
          for {
            _ <- Update0(createSchemaSQL.getLines().mkString("\n"), None).run.transact(tnx)
            _ <- insertProjects.update.run.transact(tnx)
            _ <- insertTasks.update.run.transact(tnx)
            _ <- insertStatistics.update.run.transact(tnx)
          } yield ()
      )
    zio.Runtime.default.unsafeRunTask(task)
  }

  override def beforeStop(): Unit = {
    ds.close()
  }

}
