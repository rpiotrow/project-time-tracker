package io.github.rpiotrow.ptt.write.repository

import java.time.{Clock, LocalDateTime, ZoneOffset}
import java.util.UUID

import cats.effect._
import com.dimafeng.testcontainers.{ForAllTestContainer, JdbcDatabaseContainer, PostgreSQLContainer}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.Transactor
import doobie.implicits._
import doobie.util.ExecutionContexts
import doobie.util.update.Update0
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers._

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.io.Source

class AllRepositoriesSpec
    extends AnyFunSpec
    with ForAllTestContainer
    with should.Matchers
    with ProjectRepositorySpec
    with ProjectReadSideRepositorySpec {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  override val container = new PostgreSQLContainer(
    commonJdbcParams = JdbcDatabaseContainer.CommonParams(startupTimeout = 240.seconds, connectTimeout = 240.seconds)
  )

  override val owner1Id = UUID.fromString("41a854e4-4262-4672-a7df-c781f535d6ee")

  private lazy val hikariConfig = {
    val hc = new HikariConfig()
    hc.setDriverClassName("org.postgresql.Driver")
    hc.setJdbcUrl(container.jdbcUrl)
    hc.setUsername("writer")
    hc.setPassword("writer")
    hc
  }
  private lazy val ds     = new HikariDataSource(hikariConfig)
  override lazy val tnx   = Transactor
    .fromDataSource[IO](ds, ExecutionContexts.synchronous, Blocker.liftExecutionContext(ExecutionContexts.synchronous))
  override lazy val now   = LocalDateTime.of(2015, 2, 13, 14, 23)
  override lazy val clock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

  override lazy val projectRepo         = new ProjectRepositoryLive(liveContext, clock)
  override lazy val projectReadSideRepo = new ProjectReadSideRepositoryLive(liveContext)

  override def afterStart(): Unit = {
    val tnx = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      container.jdbcUrl,
      container.username,
      container.password,
      Blocker.liftExecutionContext(ExecutionContexts.synchronous)
    )

    Resource
      .make(IO(Source.fromFile("local-dev/create-schema.sql")))(s => IO(s.close()))
      .use(createSchemaSQL => Update0(createSchemaSQL.getLines().mkString("\n"), None).run.transact(tnx))
      .unsafeRunSync()
  }

  override def beforeStop(): Unit = {
    ds.close()
  }

}
