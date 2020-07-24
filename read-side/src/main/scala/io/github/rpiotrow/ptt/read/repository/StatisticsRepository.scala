package io.github.rpiotrow.ptt.read.repository

import java.util.UUID

import doobie.Transactor
import doobie.implicits._
import doobie.quill.DoobieContext
import io.getquill.{idiom => _, _}
import io.github.rpiotrow.ptt.read.entity.StatisticsEntity
import zio._
import zio.interop.catz._

case class YearMonth(year: Int, month: Int)
case class YearMonthRange(from: YearMonth, to: YearMonth)

object StatisticsRepository {
  trait Service {
    def read(owners: List[UUID], range: YearMonthRange): Task[List[StatisticsEntity]]
  }
  def live(tnx: Transactor[Task]): Service =
    new Service {
      private val dc = new DoobieContext.Postgres(SnakeCase) with DurationDecoder
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
}
