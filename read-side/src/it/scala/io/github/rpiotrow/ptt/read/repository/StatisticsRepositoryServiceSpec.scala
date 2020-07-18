package io.github.rpiotrow.ptt.read.repository

import java.util.UUID

import cats.effect.Blocker
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.implicits._
import doobie.util.ExecutionContexts
import doobie.Transactor
import doobie.util.update.Update0
import io.github.rpiotrow.ptt.read.entity.StatisticsEntity
import org.scalatest.matchers._
import org.scalatest.wordspec.AnyWordSpec
import zio._
import zio.interop.catz._

import scala.io.Source

class StatisticsRepositoryServiceSpec extends AnyWordSpec with ForAllTestContainer with should.Matchers {

  override val container = PostgreSQLContainer()

  private val owner1Id = UUID.fromString("41a854e4-4262-4672-a7df-c781f535d6ee")
  private val owner2Id = UUID.fromString("66ffc00e-083b-48aa-abb5-8ef46ac0e06e")

  override def afterStart(): Unit = {
    val xa         = Transactor.fromDriverManager[Task](
      "org.postgresql.Driver",
      container.jdbcUrl,
      container.username,
      container.password,
      Blocker.liftExecutionContext(ExecutionContexts.synchronous)
    )
    val insertData = sql"""
      |INSERT INTO ptt_read_model.statistics(owner, year, month, number_of_tasks, average_task_duration, average_task_volume, volume_weighted_average_task_duration)
      |  VALUES ('41a854e4-4262-4672-a7df-c781f535d6ee', 2020, 7, 1, 2, 3, 4),
      |    ('41a854e4-4262-4672-a7df-c781f535d6ee', 2020, 8, 1, 2, 3, 4),
      |    ('66ffc00e-083b-48aa-abb5-8ef46ac0e06e', 2020, 7, 4, 3, 2, 1)
      |;
      |""".stripMargin

    val task = ZManaged
      .make(IO(Source.fromFile("local-dev/create-schema.sql")))(s => IO(s.close()).orDie)
      .use(
        createSchemaSQL =>
          for {
            _ <- Update0(createSchemaSQL.getLines().mkString("\n"), None).run.transact(xa)
            _ <- insertData.update.run.transact(xa)
          } yield ()
      )
    zio.Runtime.default.unsafeRunTask(task)
  }

  private lazy val hikariConfig = {
    val hc = new HikariConfig()
    hc.setDriverClassName("org.postgresql.Driver")
    hc.setJdbcUrl(container.jdbcUrl)
    hc.setUsername("reader")
    hc.setPassword("reader")
    hc.setSchema("ptt_read_model")
    hc
  }
  private lazy val ds      = new HikariDataSource(hikariConfig)
  private lazy val xa      = Transactor.fromDataSource[Task](
    ds,
    ExecutionContexts.synchronous,
    Blocker.liftExecutionContext(ExecutionContexts.synchronous)
  )
  private lazy val service = new StatisticsRepositoryService(xa)

  override def beforeStop(): Unit = {
    ds.close()
  }

  "StatisticsRepositoryService" when {
    "read is invoked" should {
      "return list for both owners" in {
        val result = zio.Runtime.default.unsafeRunTask(
          service.read(List(owner1Id, owner2Id), YearMonthRange(YearMonth(2020, 1), YearMonth(2020, 12)))
        )
        result should be(
          List(
            StatisticsEntity(1, owner1Id, 2020, 7, 1, 2, 3, 4),
            StatisticsEntity(2, owner1Id, 2020, 8, 1, 2, 3, 4),
            StatisticsEntity(3, owner2Id, 2020, 7, 4, 3, 2, 1)
          )
        )
      }
      "return list for one owner" in {
        val result = zio.Runtime.default.unsafeRunTask(
          service.read(List(owner2Id), YearMonthRange(YearMonth(2020, 1), YearMonth(2020, 12)))
        )
        result should be(List(StatisticsEntity(3, owner2Id, 2020, 7, 4, 3, 2, 1)))
      }
      "return list for one month" in {
        val result = zio.Runtime.default.unsafeRunTask(
          service.read(List(owner1Id, owner2Id), YearMonthRange(YearMonth(2020, 7), YearMonth(2020, 7)))
        )
        result should be(
          List(StatisticsEntity(1, owner1Id, 2020, 7, 1, 2, 3, 4), StatisticsEntity(3, owner2Id, 2020, 7, 4, 3, 2, 1))
        )
      }
      "return empty list for unknown owner" in {
        val result = zio.Runtime.default.unsafeRunTask(
          service.read(List(UUID.randomUUID()), YearMonthRange(YearMonth(2020, 1), YearMonth(2020, 12)))
        )
        result should be(List())
      }
    }
  }

}
