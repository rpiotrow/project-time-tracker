package io.github.rpiotrow.ptt.e2e

import java.time.temporal.ChronoUnit
import java.time.{Duration, OffsetDateTime, YearMonth}

import io.github.rpiotrow.ptt.api.input._
import io.github.rpiotrow.ptt.api.model._
import io.github.rpiotrow.ptt.api.output._
import io.github.rpiotrow.ptt.api.param._
import io.github.rpiotrow.ptt.e2e.factories.TaskFactory._
import io.github.rpiotrow.ptt.e2e.factories.UserIdFactory._
import io.github.rpiotrow.ptt.e2e.utils._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

import scala.math.BigDecimal.RoundingMode

class StatisticsEnd2EndTests extends AnyFunSpec with should.Matchers with End2EndTestsBase with ApiResults {

  import ApiClient._

  describe("statistics") {
    it("empty") {
      val userId     = generateUserId()
      val projectIds = NonEmptyUserIdList.of(generateUserId())

      val emptyYear = statistics(
        StatisticsParams(ids = projectIds, from = YearMonth.of(2020, 1), to = YearMonth.of(2020, 12)),
        userId
      ).success()
      emptyYear should be(StatisticsOutput.ZERO)

      val emptyMonth =
        statistics(StatisticsParams(ids = projectIds, from = YearMonth.of(2020, 8), to = YearMonth.of(2020, 8)), userId)
          .success()
      emptyMonth should be(StatisticsOutput.ZERO)
    }
    it("one task") {
      val (projectId: ProjectId, userId: UserId) = createProjectWithOwner

      val taskInput = TaskInput(
        startedAt = OffsetDateTime.parse("2020-09-10T09:00:00Z"),
        duration = Duration.ofHours(8),
        volume = Some(5),
        comment = None
      )
      val taskId    = extractTaskId(createTask(projectId, taskInput, userId).success(), projectId)

      val params =
        StatisticsParams(ids = NonEmptyUserIdList.of(userId), from = YearMonth.of(2020, 9), to = YearMonth.of(2020, 9))

      val oneTask = statistics(params, userId).success()
      oneTask should be(StatisticsOutput(1, Some(Duration.ofHours(8)), Some(5), Some(Duration.ofHours(8))))

      deleteTask(projectId, taskId, userId).success()

      val empty = statistics(params, userId).success()
      empty should be(StatisticsOutput.ZERO)
    }
    it("few tasks") {
      val (projectId: ProjectId, userId: UserId) = createProjectWithOwner

      val taskInput1 = TaskInput(
        startedAt = OffsetDateTime.parse("2020-09-10T09:00:00Z"),
        duration = Duration.ofHours(8),
        volume = Some(5),
        comment = None
      )
      val taskInput2 = TaskInput(
        startedAt = OffsetDateTime.parse("2020-09-11T09:00:00Z"),
        duration = Duration.ofHours(4),
        volume = Some(3),
        comment = None
      )
      val taskInput3 = TaskInput(
        startedAt = OffsetDateTime.parse("2020-09-12T09:00:00Z"),
        duration = Duration.ofHours(2),
        volume = Some(8),
        comment = None
      )
      createTask(projectId, taskInput1, userId).success()
      val taskId     = extractTaskId(createTask(projectId, taskInput2, userId).success(), projectId)
      createTask(projectId, taskInput3, userId).success()

      val params =
        StatisticsParams(ids = NonEmptyUserIdList.of(userId), from = YearMonth.of(2020, 9), to = YearMonth.of(2020, 9))

      val statisticsFor3 = statistics(params, userId).success()
      val volumeAverage3 = BigDecimal(16.0 / 3.0).setScale(2, RoundingMode.HALF_UP)
      statisticsFor3 should be(
        StatisticsOutput(
          3,
          Some(Duration.ofHours(14).dividedBy(3).truncatedTo(ChronoUnit.SECONDS)),
          Some(volumeAverage3),
          Some(Duration.ofHours(68).dividedBy(16).truncatedTo(ChronoUnit.SECONDS))
        )
      )

      deleteTask(projectId, taskId, userId).success()
      val statisticsFor2 = statistics(params, userId).success()
      val volumeAverage2 = BigDecimal(13.0 / 2.0).setScale(2, RoundingMode.HALF_UP)
      statisticsFor2 should be(
        StatisticsOutput(
          2,
          Some(Duration.ofHours(10).dividedBy(2).truncatedTo(ChronoUnit.SECONDS)),
          Some(volumeAverage2),
          Some(Duration.ofHours(56).dividedBy(13).truncatedTo(ChronoUnit.SECONDS))
        )
      )
    }
  }

}
