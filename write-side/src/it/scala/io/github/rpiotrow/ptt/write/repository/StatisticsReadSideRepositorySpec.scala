package io.github.rpiotrow.ptt.write.repository

import java.time.{Duration, YearMonth}
import java.util.UUID
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits._
import com.softwaremill.diffx.generic.auto._
import com.softwaremill.diffx.scalatest.DiffShouldMatcher._
import com.softwaremill.diffx.{Derived, Diff}
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment.Fragment
import io.github.rpiotrow.ptt.api.model.UserId
import io.github.rpiotrow.ptt.write.entity.StatisticsReadSideEntity
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

trait StatisticsReadSideRepositorySpec { this: AnyFunSpec with should.Matchers =>

  protected def tnx: Transactor[IO]
  protected def statisticsReadSideRepo: StatisticsReadSideRepository

  protected val statisticsReadSideRepositoryData: Fragment =
    sql"""
         |INSERT INTO ptt_read_model.statistics(db_id, owner, year, month, number_of_tasks, number_of_tasks_with_volume, duration_sum, volume_sum, volume_weighted_task_duration_sum)
         |  VALUES (100, '41a854e4-4262-4672-a7df-c781f535d6ee', 2020, 7, 1, 1, 7200, 3, 21600),
         |    (101, '41a854e4-4262-4672-a7df-c781f535d6ee', 2020, 8, 1, NULL, 7200, NULL, NULL)
         |;
         |""".stripMargin
  private val taskOwner                          = UserId("41a854e4-4262-4672-a7df-c781f535d6ee")
  lazy private val s1                            =
    StatisticsReadSideEntity(
      dbId = 100,
      owner = taskOwner,
      year = 2020,
      month = 7,
      numberOfTasks = 1,
      numberOfTasksWithVolume = 1.some,
      durationSum = Duration.ofMinutes(120),
      volumeSum = 3L.some,
      volumeWeightedTaskDurationSum = Duration.ofMinutes(360).some
    )
  lazy private val s2                            =
    StatisticsReadSideEntity(
      dbId = 101,
      owner = taskOwner,
      year = 2020,
      month = 8,
      numberOfTasks = 1,
      numberOfTasksWithVolume = None,
      durationSum = Duration.ofMinutes(120),
      volumeSum = None,
      volumeWeightedTaskDurationSum = None
    )

  describe("StatisticsReadSideRepository") {
    describe("get should") {
      it("return existing") {
        val result = statisticsReadSideRepo
          .get(taskOwner, YearMonth.of(2020, 7))
          .transact(tnx)
          .unsafeRunSync()

        result shouldMatchTo(s1.some)
      }
      it("return none when does not exist") {
        val result = statisticsReadSideRepo
          .get(taskOwner, YearMonth.of(2021, 7))
          .transact(tnx)
          .unsafeRunSync()

        result should be(None)
      }
    }
    describe("upsert should") {
      it("insert new statistics") {
        val statistics = s1.copy(dbId = 0, year = 2022, numberOfTasks = 4)
        statisticsReadSideRepo.upsert(statistics).transact(tnx).unsafeRunSync()
        val read       = statisticsReadSideRepo
          .get(taskOwner, YearMonth.of(2022, 7))
          .transact(tnx)
          .unsafeRunSync()

        implicit val ignoreDbId: Diff[StatisticsReadSideEntity] =
          Derived[Diff[StatisticsReadSideEntity]].ignore(_.dbId)
        read shouldMatchTo(statistics.some)
      }
      it("update existing statistics") {
        val statistics = s2.copy(numberOfTasks = 7)
        statisticsReadSideRepo.upsert(statistics).transact(tnx).unsafeRunSync()
        val read       = statisticsReadSideRepo
          .get(taskOwner, YearMonth.of(2020, 8))
          .transact(tnx)
          .unsafeRunSync()

        read shouldMatchTo(statistics.some)
      }
    }
  }

}
