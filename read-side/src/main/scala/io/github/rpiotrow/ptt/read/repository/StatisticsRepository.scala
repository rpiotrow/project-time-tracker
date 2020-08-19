package io.github.rpiotrow.ptt.read.repository

import java.util.UUID

import doobie.Transactor
import doobie.implicits._
import doobie.quill.DoobieContext
import io.getquill.{idiom => _, _}
import io.github.rpiotrow.ptt.api.param.StatisticsParams
import io.github.rpiotrow.ptt.read.entity.StatisticsEntity
import zio._
import zio.interop.catz._

object StatisticsRepository {
  trait Service {
    def list(params: StatisticsParams): IO[RepositoryThrowable, List[StatisticsEntity]]
  }
  def live(tnx: Transactor[Task]): Service =
    new Service {
      private val dc = new DoobieContext.Postgres(SnakeCase) with DurationDecoder
      import dc._

      private val statistics = quote { querySchema[StatisticsEntity]("statistics") }

      override def list(params: StatisticsParams) = {
        run(quote {
          statistics
            .filter(e => liftQuery(params.ids.toList).contains(e.owner))
            .filter(e => (e.year >= lift(params.from.getYear)) && (e.year <= lift(params.to.getYear)))
            .filter(e => (e.month >= lift(params.from.getMonthValue)) && (e.month <= lift(params.to.getMonthValue)))
        })
          .transact(tnx)
          .mapError(RepositoryThrowable)
      }
    }
}
