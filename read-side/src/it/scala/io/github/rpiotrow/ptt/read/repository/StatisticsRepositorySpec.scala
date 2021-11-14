package io.github.rpiotrow.ptt.read.repository

import java.util.UUID
import cats.implicits._
import com.softwaremill.diffx.generic.auto._
import com.softwaremill.diffx.scalatest.DiffMatcher._
import io.github.rpiotrow.ptt.read.entity.StatisticsEntity
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should
import doobie.implicits._
import io.github.rpiotrow.ptt.api.param.StatisticsParams

import java.time.{Duration, YearMonth}
import cats.data.NonEmptyList
import doobie.util.fragment.Fragment
import io.github.rpiotrow.ptt.api.model.{NonEmptyUserIdList, UserId}

trait StatisticsRepositorySpec { this: AnyFunSpec with should.Matchers =>

  def owner1Id: UserId
  def owner2Id: UserId

  def statisticsRepo: StatisticsRepository.Service

  val insertStatistics: Fragment = sql"""
    |INSERT INTO ptt_read_model.statistics(owner, year, month, number_of_tasks, number_of_tasks_with_volume, duration_sum, volume_sum, volume_weighted_task_duration_sum)
    |  VALUES ('41a854e4-4262-4672-a7df-c781f535d6ee', 2020, 7, 1, 1, 7200, 3, 21600),
    |    ('41a854e4-4262-4672-a7df-c781f535d6ee', 2020, 8, 1, NULL, 7200, NULL, NULL),
    |    ('66ffc00e-083b-48aa-abb5-8ef46ac0e06e', 2020, 7, 4, 3, 10800, 5, 7500)
    |;
    |""".stripMargin

  lazy val s1 =
    StatisticsEntity(owner1Id, 2020, 7, 1, 1.some, Duration.ofMinutes(120), 3L.some, Duration.ofMinutes(360).some)
  lazy val s2 =
    StatisticsEntity(owner1Id, 2020, 8, 1, None, Duration.ofMinutes(120), None, None)
  lazy val s3 =
    StatisticsEntity(owner2Id, 2020, 7, 4, 3.some, Duration.ofMinutes(180), 5L.some, Duration.ofMinutes(125).some)

  describe("StatisticsRepository list() should") {
    it("return list for both owners") {
      val params =
        StatisticsParams(NonEmptyUserIdList.of(owner1Id, owner2Id), YearMonth.of(2020, 1), YearMonth.of(2020, 12))
      val result = zio.Runtime.default.unsafeRun(statisticsRepo.list(params))
      result should matchTo(List(s1, s2, s3))
    }
    it("return list for one owner") {
      val params = StatisticsParams(NonEmptyUserIdList.of(owner2Id), YearMonth.of(2020, 1), YearMonth.of(2020, 12))
      val result = zio.Runtime.default.unsafeRun(statisticsRepo.list(params))
      result should matchTo(List(s3))
    }
    it("return list for one month") {
      val params =
        StatisticsParams(NonEmptyUserIdList.of(owner1Id, owner2Id), YearMonth.of(2020, 7), YearMonth.of(2020, 7))
      val result = zio.Runtime.default.unsafeRun(statisticsRepo.list(params))
      result should matchTo(List(s1, s3))
    }
    it("return empty list for unknown owner") {
      val params = StatisticsParams(
        NonEmptyUserIdList.of(UserId(UUID.randomUUID())),
        YearMonth.of(2020, 1),
        YearMonth.of(2020, 12)
      )
      val result = zio.Runtime.default.unsafeRun(statisticsRepo.list(params))
      result should be(List())
    }
  }

}
