package io.github.rpiotrow.ptt.read.repository

import java.time.Duration
import java.util.UUID

import com.softwaremill.diffx.scalatest.DiffMatcher._
import io.github.rpiotrow.ptt.read.entity.StatisticsEntity
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should
import doobie.implicits._

trait StatisticsRepositorySpec { this: AnyFunSpec with should.Matchers =>

  def owner1Id: UUID
  def owner2Id: UUID

  def statisticsRepo: StatisticsRepository.Service

  val insertStatistics = sql"""
    |INSERT INTO ptt_read_model.statistics(owner, year, month, number_of_tasks, average_task_duration, average_task_volume, volume_weighted_average_task_duration)
    |  VALUES ('41a854e4-4262-4672-a7df-c781f535d6ee', 2020, 7, 1, '2 hours', 3, '4 hours'),
    |    ('41a854e4-4262-4672-a7df-c781f535d6ee', 2020, 8, 1, '2 hours', 3, '4 hours'),
    |    ('66ffc00e-083b-48aa-abb5-8ef46ac0e06e', 2020, 7, 4, '3 hours', 2, '1 hour')
    |;
    |""".stripMargin

  lazy val s1 = StatisticsEntity(owner1Id, 2020, 7, 1, Duration.ofHours(2), 3, Duration.ofHours(4))
  lazy val s2 = StatisticsEntity(owner1Id, 2020, 8, 1, Duration.ofHours(2), 3, Duration.ofHours(4))
  lazy val s3 = StatisticsEntity(owner2Id, 2020, 7, 4, Duration.ofHours(3), 2, Duration.ofHours(1))

  describe("StatisticsRepository read() should") {
    it("return list for both owners") {
      val result = zio.Runtime.default.unsafeRunTask(
        statisticsRepo.read(List(owner1Id, owner2Id), YearMonthRange(YearMonth(2020, 1), YearMonth(2020, 12)))
      )
      result should matchTo(List(s1, s2, s3))
    }
    it("return list for one owner") {
      val result = zio.Runtime.default.unsafeRunTask(
        statisticsRepo.read(List(owner2Id), YearMonthRange(YearMonth(2020, 1), YearMonth(2020, 12)))
      )
      result should matchTo(List(s3))
    }
    it("return list for one month") {
      val result = zio.Runtime.default.unsafeRunTask(
        statisticsRepo.read(List(owner1Id, owner2Id), YearMonthRange(YearMonth(2020, 7), YearMonth(2020, 7)))
      )
      result should matchTo(List(s1, s3))
    }
    it("return empty list for unknown owner") {
      val result = zio.Runtime.default.unsafeRunTask(
        statisticsRepo.read(List(UUID.randomUUID()), YearMonthRange(YearMonth(2020, 1), YearMonth(2020, 12)))
      )
      result should be(List())
    }
  }

}
