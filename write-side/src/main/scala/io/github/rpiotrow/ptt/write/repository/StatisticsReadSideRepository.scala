package io.github.rpiotrow.ptt.write.repository

import java.time.YearMonth

import io.github.rpiotrow.ptt.api.model.UserId
import io.github.rpiotrow.ptt.write.entity.StatisticsReadSideEntity

trait StatisticsReadSideRepository {
  def get(owner: UserId, yearMonth: YearMonth): DBResult[Option[StatisticsReadSideEntity]]
  def upsert(statistics: StatisticsReadSideEntity): DBResult[Unit]
}

object StatisticsReadSideRepository {
  val live: StatisticsReadSideRepository = new StatisticsReadSideRepositoryLive(liveContext)
}

private[repository] class StatisticsReadSideRepositoryLive(ctx: DBContext)
    extends StatisticsReadSideRepository
    with ReadSideRepositoryBase {

  import ctx._

  private val statistics = quote { querySchema[StatisticsReadSideEntity]("ptt_read_model.statistics") }

  override def get(owner: UserId, yearMonth: YearMonth): DBResult[Option[StatisticsReadSideEntity]] = {
    run(quote {
      statistics
        .filter(_.owner == lift(owner))
        .filter(_.year == lift(yearMonth.getYear))
        .filter(_.month == lift(yearMonth.getMonthValue))
    }).map(_.headOption)
  }

  override def upsert(entity: StatisticsReadSideEntity): DBResult[Unit] = {
    if (entity.dbId == 0) {
      run(quote {
        statistics.insert(lift(entity)).returningGenerated(_.dbId)
      }).map(logIfNotUpdated(s"cannot insert statistics for ${entity.owner} ${entity.year}-${entity.month}"))
    } else {
      run(quote {
        statistics.filter(_.dbId == lift(entity.dbId)).update(lift(entity))
      }).map(
        logIfNotUpdated(
          s"cannot update statistics for ${entity.owner} ${entity.year}-${entity.month} with dbId ${entity.dbId}"
        )
      )
    }
  }

}
