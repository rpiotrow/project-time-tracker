package io.github.rpiotrow.ptt.read

import java.util.UUID

import io.github.rpiotrow.ptt.read.configuration.DatabaseConfiguration
import io.github.rpiotrow.ptt.read.entity.StatisticsEntity
import zio.blocking.Blocking
import zio._
import zio.config.Config

import scala.concurrent.ExecutionContext

package object repository {
  object StatisticsRepository {
    trait Service {
      def read(owners: List[UUID], range: YearMonthRange): Task[List[StatisticsEntity]]
    }
  }

  type StatisticsRepository    = Has[StatisticsRepository.Service]
  type StatisticsRepositoryEnv = Blocking with Config[DatabaseConfiguration]

  def postgreSQL(connectEC: ExecutionContext): ZLayer[StatisticsRepositoryEnv, Throwable, StatisticsRepository] =
    ZLayer.fromManaged(for {
      blockingEC    <- blocking.blocking { ZIO.descriptor.map(_.executor.asEC) }.toManaged_
      configuration <- zio.config.config[DatabaseConfiguration].toManaged_
      managed       <- StatisticsRepositoryService.create(configuration, connectEC, blockingEC)
    } yield managed)

  def read(owners: List[UUID], range: YearMonthRange): ZIO[StatisticsRepository, Throwable, List[StatisticsEntity]] =
    ZIO.accessM(_.get.read(owners, range))

}
