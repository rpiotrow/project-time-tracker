package io.github.rpiotrow.ptt.read.repository

import java.util.UUID

import cats.effect.Blocker
import com.zaxxer.hikari.HikariConfig
import doobie.Transactor
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.quill.DoobieContext
import io.getquill.{idiom => _, _}
import io.github.rpiotrow.ptt.read.configuration.DatabaseConfiguration
import io.github.rpiotrow.ptt.read.entity.StatisticsEntity
import zio._
import zio.interop.catz._

import scala.concurrent.ExecutionContext

case class YearMonth(year: Int, month: Int)
case class YearMonthRange(from: YearMonth, to: YearMonth)

class StatisticsRepositoryService(tnx: Transactor[Task]) extends StatisticsRepository.Service {
  private val dc = new DoobieContext.Postgres(SnakeCase)
  import dc._

  private val statistics = quote { querySchema[StatisticsEntity]("statistics") }

  override def read(owners: List[UUID], range: YearMonthRange): Task[List[StatisticsEntity]] = {
    run(quote {
      statistics
        .filter(e => liftQuery(owners).contains(e.owner))
        .filter(e => (e.year >= lift(range.from.year)) && (e.year <= lift(range.to.year)))
        .filter(e => (e.month >= lift(range.from.month)) && (e.month <= lift(range.to.month)))
    }).transact(tnx)
  }

}

object StatisticsRepositoryService {
  def create(
    configuration: DatabaseConfiguration,
    connectEC: ExecutionContext,
    transactEC: ExecutionContext
  ): Managed[Throwable, StatisticsRepositoryService] = {

    val hikariConfig = new HikariConfig()
    hikariConfig.setDriverClassName(configuration.jdbcDriver)
    hikariConfig.setJdbcUrl(configuration.jdbcUrl)
    hikariConfig.setUsername(configuration.dbUsername)
    hikariConfig.setPassword(configuration.dbPassword)
    hikariConfig.setSchema(configuration.schema)

    HikariTransactor
      .fromHikariConfig[Task](hikariConfig, connectEC, Blocker.liftExecutionContext(transactEC))
      .toManagedZIO
      .map(new StatisticsRepositoryService(_))
  }
}
