package io.github.rpiotrow.ptt.write.service

import java.time.{Duration, LocalDateTime, YearMonth}

import com.softwaremill.diffx.scalatest.DiffMatcher._
import cats.implicits._
import doobie.implicits._
import io.github.rpiotrow.ptt.write.entity.{StatisticsReadSideEntity, TaskEntity}
import io.github.rpiotrow.ptt.write.repository.{
  DBResult,
  ProjectReadSideRepository,
  StatisticsReadSideRepository,
  TaskReadSideRepository
}
import org.scalamock.matchers.ArgCapture.{CaptureAll, CaptureOne}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class ReadSideServiceSpec extends AnyFunSpec with ServiceSpecBase with MockFactory with should.Matchers {

  describe("projectCreated should") {
    it("create project in read model") {
      val projectReadSideRepository    = mock[ProjectReadSideRepository]
      val taskReadSideRepository       = mock[TaskReadSideRepository]
      val statisticsReadSideRepository = mock[StatisticsReadSideRepository]
      val service                      =
        new ReadSideServiceLive(projectReadSideRepository, taskReadSideRepository, statisticsReadSideRepository)

      (projectReadSideRepository.newProject _).expects(project).returning(projectReadModel.pure[DBResult])
      val result = service.projectCreated(project).transact(tnx).value.unsafeRunSync()

      result should be(projectReadModel.asRight[AppFailure])
    }
  }

  describe("projectDeleted should") {
    it("update project, tasks and statistics in the read model") {
      val projectReadSideRepository    = mock[ProjectReadSideRepository]
      val taskReadSideRepository       = mock[TaskReadSideRepository]
      val statisticsReadSideRepository = mock[StatisticsReadSideRepository]
      val service                      =
        new ReadSideServiceLive(projectReadSideRepository, taskReadSideRepository, statisticsReadSideRepository)

      (projectReadSideRepository.deleteProject _).expects(project).returning(().pure[DBResult])
      // TODO: delete all tasks related to project on read side
      // TODO: update statistics
      val result = service.projectDeleted(project).transact(tnx).value.unsafeRunSync()

      result should be(().asRight[AppFailure])
    }
  }

  describe("newTask should") {
    it("update project and task in the read model") {
      val (service, statisticsReadSideRepository) = taskAddedPrepare()
      (statisticsReadSideRepository.get _)
        .expects(ownerId, taskStartedAtYearMonth)
        .returning(Option.empty[StatisticsReadSideEntity].pure[DBResult])
      (statisticsReadSideRepository.upsert _).expects(*).returning(().pure[DBResult])

      val result = service.taskAdded(task).transact(tnx).value.unsafeRunSync()

      result should be(taskReadModel.asRight[AppFailure])
    }
    it("create initial statistics") {
      val (service, statisticsReadSideRepository) = taskAddedPrepare()
      (statisticsReadSideRepository.get _)
        .expects(ownerId, taskStartedAtYearMonth)
        .returning(Option.empty[StatisticsReadSideEntity].pure[DBResult])
      val stats                                   = createStatistics(
        numberOfTasks = 1,
        numberOfTasksWithVolume = 1,
        durationSum = taskDuration,
        volumeWeightedTaskDurationSum = taskVolumeWeightedDuration.some,
        volumeSum = BigDecimal(taskVolume).some
      )
      (statisticsReadSideRepository.upsert _).expects(stats).returning(().pure[DBResult])

      service.taskAdded(task).transact(tnx).value.unsafeRunSync()
    }
    it("update existing statistics") {
      val (service, statisticsReadSideRepository) = taskAddedPrepare()
      val currentStats                            = createStatistics(
        numberOfTasks = 10,
        numberOfTasksWithVolume = 5,
        durationSum = Duration.ofMinutes(480),
        volumeWeightedTaskDurationSum = Duration.ofMinutes(885).some,
        volumeSum = BigDecimal(23).some
      )
      (statisticsReadSideRepository.get _)
        .expects(ownerId, taskStartedAtYearMonth)
        .returning(Option(currentStats).pure[DBResult])
      val newStats                                = createStatistics(
        numberOfTasks = 11,
        numberOfTasksWithVolume = 6,
        durationSum = Duration.ofMinutes(510),
        volumeWeightedTaskDurationSum = Duration.ofMinutes(1185).some,
        volumeSum = BigDecimal(33).some
      )
      val argCaptor                               = CaptureOne[StatisticsReadSideEntity]()
      statisticsReadSideRepository.upsert _ expects capture(argCaptor) returning ().pure[DBResult]

      service.taskAdded(task).transact(tnx).value.unsafeRunSync()
      argCaptor.value should matchTo(newStats)
    }
    it("create initial statistics for two months") {
      val lateTask                                = task.copy(
        startedAt = LocalDateTime.parse("2020-08-31T23:00:00"),
        duration = Duration.ofHours(2),
        volume = None,
        comment = None
      )
      val (service, statisticsReadSideRepository) = taskAddedPrepare(lateTask, lateTask)
      (statisticsReadSideRepository.get _)
        .expects(ownerId, YearMonth.of(2020, 8))
        .returning(Option.empty[StatisticsReadSideEntity].pure[DBResult])
      (statisticsReadSideRepository.get _)
        .expects(ownerId, YearMonth.of(2020, 9))
        .returning(Option.empty[StatisticsReadSideEntity].pure[DBResult])
      val argCaptor                               = CaptureAll[StatisticsReadSideEntity]()
      statisticsReadSideRepository.upsert _ expects capture(argCaptor) repeat 2 returning ().pure[DBResult]

      service.taskAdded(lateTask).transact(tnx).value.unsafeRunSync()

      val stats8 = createStatistics(
        year = 2020,
        month = 8,
        numberOfTasks = 1,
        numberOfTasksWithVolume = 0,
        durationSum = Duration.ofHours(1)
      )
      val stats9 = createStatistics(
        year = 2020,
        month = 9,
        numberOfTasks = 1,
        numberOfTasksWithVolume = 0,
        durationSum = Duration.ofHours(1)
      )
      argCaptor.values should matchTo(Seq(stats8, stats9))
    }
  }

  private def taskAddedPrepare(task: TaskEntity = task, taskReadModel: TaskEntity = taskReadModel) = {
    val projectReadSideRepository    = mock[ProjectReadSideRepository]
    val taskReadSideRepository       = mock[TaskReadSideRepository]
    val statisticsReadSideRepository = mock[StatisticsReadSideRepository]
    val service                      =
      new ReadSideServiceLive(projectReadSideRepository, taskReadSideRepository, statisticsReadSideRepository)

    (taskReadSideRepository.add _).expects(task).returning(taskReadModel.pure[DBResult])
    (projectReadSideRepository.addToProjectDuration _).expects(taskReadModel).returning(().pure[DBResult])

    (service, statisticsReadSideRepository)
  }

  private def createStatistics(
    numberOfTasks: Int,
    numberOfTasksWithVolume: Int,
    durationSum: Duration,
    volumeWeightedTaskDurationSum: Option[Duration] = None,
    volumeSum: Option[BigDecimal] = None,
    year: Int = taskStartedAtYearMonth.getYear,
    month: Int = taskStartedAtYearMonth.getMonthValue
  ) =
    StatisticsReadSideEntity(
      dbId = 0,
      owner = ownerId,
      year = year,
      month = month,
      numberOfTasks = numberOfTasks,
      numberOfTasksWithVolume = numberOfTasksWithVolume,
      durationSum = durationSum,
      volumeWeightedTaskDurationSum = volumeWeightedTaskDurationSum,
      volumeSum = volumeSum
    )

}
