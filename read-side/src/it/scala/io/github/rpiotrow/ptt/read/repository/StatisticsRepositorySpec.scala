package io.github.rpiotrow.ptt.read.repository

import java.util.UUID

import com.softwaremill.diffx.scalatest.DiffMatcher._
import io.github.rpiotrow.ptt.read.entity.StatisticsEntity
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should
import doobie.implicits._
import io.github.rpiotrow.projecttimetracker.api.param.StatisticsParams
import java.time.YearMonth

import cats.data.NonEmptyList

trait StatisticsRepositorySpec { this: AnyFunSpec with should.Matchers =>

  def owner1Id: UUID
  def owner2Id: UUID

  def statisticsRepo: StatisticsRepository.Service

  val insertStatistics = sql"""
    |INSERT INTO ptt_read_model.statistics(owner, year, month, number_of_tasks, average_task_duration_minutes, average_task_volume, volume_weighted_task_duration_sum, volume_sum)
    |  VALUES ('41a854e4-4262-4672-a7df-c781f535d6ee', 2020, 7, 1, 120, 3, 1.2, 1.3),
    |    ('41a854e4-4262-4672-a7df-c781f535d6ee', 2020, 8, 1, 120, 3, 1.4, 1.5),
    |    ('66ffc00e-083b-48aa-abb5-8ef46ac0e06e', 2020, 7, 4, 180, 2, 1.6, 1.7)
    |;
    |""".stripMargin

  lazy val s1 = StatisticsEntity(owner1Id, 2020, 7, 1, 120, 3, 1.2, 1.3)
  lazy val s2 = StatisticsEntity(owner1Id, 2020, 8, 1, 120, 3, 1.4, 1.5)
  lazy val s3 = StatisticsEntity(owner2Id, 2020, 7, 4, 180, 2, 1.6, 1.7)

  describe("StatisticsRepository list() should") {
    it("return list for both owners") {
      val params = StatisticsParams(NonEmptyList.of(owner1Id, owner2Id), YearMonth.of(2020, 1), YearMonth.of(2020, 12))
      val result = zio.Runtime.default.unsafeRun(statisticsRepo.list(params))
      result should matchTo(List(s1, s2, s3))
    }
    it("return list for one owner") {
      val params = StatisticsParams(NonEmptyList.of(owner2Id), YearMonth.of(2020, 1), YearMonth.of(2020, 12))
      val result = zio.Runtime.default.unsafeRun(statisticsRepo.list(params))
      result should matchTo(List(s3))
    }
    it("return list for one month") {
      val params = StatisticsParams(NonEmptyList.of(owner1Id, owner2Id), YearMonth.of(2020, 7), YearMonth.of(2020, 7))
      val result = zio.Runtime.default.unsafeRun(statisticsRepo.list(params))
      result should matchTo(List(s1, s3))
    }
    it("return empty list for unknown owner") {
      val params = StatisticsParams(NonEmptyList.of(UUID.randomUUID()), YearMonth.of(2020, 1), YearMonth.of(2020, 12))
      val result = zio.Runtime.default.unsafeRun(statisticsRepo.list(params))
      result should be(List())
    }
  }

}
