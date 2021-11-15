package io.github.rpiotrow.ptt.read.repository

import io.getquill.context.ZioJdbc._
import io.getquill.context.qzio.ImplicitSyntax.Implicit
import io.github.rpiotrow.ptt.api.param.StatisticsParams
import io.github.rpiotrow.ptt.read.entity.StatisticsEntity
import zio.{Has, IO}

object StatisticsRepository {
  trait Service {
    def list(params: StatisticsParams): IO[RepositoryThrowable, List[StatisticsEntity]]
  }
  def live(ds: DataSourceCloseable): Service =
    new Service {
      import quillContext._
      implicit private val implicitDS: Implicit[Has[DataSourceCloseable]] = Implicit(Has(ds))

      private val statistics = quote { querySchema[StatisticsEntity]("statistics") }

      override def list(params: StatisticsParams): IO[RepositoryThrowable, List[StatisticsEntity]] = {
        run(quote {
          statistics
            .filter(e => liftQuery(params.ids.toList).contains(e.owner))
            .filter(e => (e.year >= lift(params.from.getYear)) && (e.year <= lift(params.to.getYear)))
            .filter(e => (e.month >= lift(params.from.getMonthValue)) && (e.month <= lift(params.to.getMonthValue)))
        })
          .implicitDS
          .mapError(RepositoryThrowable)
      }
    }
}
