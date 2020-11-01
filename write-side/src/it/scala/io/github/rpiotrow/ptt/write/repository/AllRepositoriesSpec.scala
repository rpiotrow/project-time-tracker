package io.github.rpiotrow.ptt.write.repository

import java.time.{Clock, Instant, ZoneOffset}

import cats.effect._
import com.dimafeng.testcontainers.{ForAllTestContainer, JdbcDatabaseContainer, PostgreSQLContainer}
import doobie.Transactor
import doobie.implicits._
import doobie.util.ExecutionContexts
import doobie.util.update.Update0
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers._

import scala.concurrent.duration.DurationInt
import scala.io.Source

class AllRepositoriesSpec
    extends AnyFunSpec
    with ForAllTestContainer
    with should.Matchers
    with ProjectRepositorySpec
    with ProjectReadSideRepositorySpec
    with StatisticsReadSideRepositorySpec
    with TaskReadSideRepositorySpec
    with TaskRepositorySpec {

  implicit protected val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  override val container = new PostgreSQLContainer(
    commonJdbcParams = JdbcDatabaseContainer.CommonParams(startupTimeout = 240.seconds, connectTimeout = 240.seconds)
  )

  override def afterStart(): Unit = {
    val tnx = makeTransactor(container.username, container.password)
    Resource
      .make(IO(Source.fromFile("local-dev/schema/create-schema.sql")))(s => IO(s.close()))
      .use(
        createSchemaSQL =>
          for {
            _ <- Update0(createSchemaSQL.getLines().mkString("\n"), None).run.transact(tnx)
            _ <- projectReadSideRepositoryData.update.run.transact(tnx)
            _ <- taskReadSideRepositoryData.update.run.transact(tnx)
            _ <- taskRepositoryData.update.run.transact(tnx)
            _ <- statisticsReadSideRepositoryData.update.run.transact(tnx)
          } yield ()
      )
      .unsafeRunSync()
  }

  override lazy protected val tnx      = makeTransactor("writer", "writer")
  override lazy protected val clockNow = Instant.parse("2015-02-13T14:23:00Z")
  private lazy val clock               = Clock.fixed(clockNow, ZoneOffset.UTC)

  override lazy protected val projectRepo            = new ProjectRepositoryLive(liveContext, clock)
  override lazy protected val projectReadSideRepo    = new ProjectReadSideRepositoryLive(liveContext)
  override lazy protected val taskRepo               = new TaskRepositoryLive(liveContext, clock)
  override lazy protected val taskReadSideRepo       = new TaskReadSideRepositoryLive(liveContext)
  override lazy protected val statisticsReadSideRepo = new StatisticsReadSideRepositoryLive(liveContext)

  private def makeTransactor(username: String, password: String) =
    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      container.jdbcUrl,
      username,
      password,
      Blocker.liftExecutionContext(ExecutionContexts.synchronous)
    )

}
