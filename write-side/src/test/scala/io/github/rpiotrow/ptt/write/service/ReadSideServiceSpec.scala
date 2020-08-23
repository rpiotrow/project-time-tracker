package io.github.rpiotrow.ptt.write.service

import java.time.{Duration, LocalDateTime, YearMonth}

import cats.Monad
import com.softwaremill.diffx.scalatest.DiffMatcher._
import cats.implicits._
import doobie.implicits._
import io.github.rpiotrow.ptt.write.entity._
import io.github.rpiotrow.ptt.write.repository._
import org.scalamock.matchers.ArgCapture.{CaptureAll, CaptureOne}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class ReadSideServiceSpec extends AnyFunSpec with ServiceSpecBase with MockFactory with should.Matchers {

  describe("ReadSideServiceSpec") {
    describe("projectCreated should") {
      it("create project in read model") {
        val projectReadSideRepository    = mock[ProjectReadSideRepository]
        val taskReadSideRepository       = mock[TaskReadSideRepository]
        val statisticsReadSideRepository = mock[StatisticsReadSideRepository]
        val service                      =
          new ReadSideServiceLive(projectReadSideRepository, taskReadSideRepository, statisticsReadSideRepository)

        (projectReadSideRepository.newProject _).expects(project).returning(projectReadModel.pure[DBResult])
        val result = service.projectCreated(project).transact(tnx).unsafeRunSync()

        result should be(projectReadModel)
      }
    }

    describe("projectUpdated should") {
      it("update project in read model") {
        val projectReadSideRepository    = mock[ProjectReadSideRepository]
        val taskReadSideRepository       = mock[TaskReadSideRepository]
        val statisticsReadSideRepository = mock[StatisticsReadSideRepository]
        val service                      =
          new ReadSideServiceLive(projectReadSideRepository, taskReadSideRepository, statisticsReadSideRepository)

        (projectReadSideRepository.updateProject _)
          .expects(projectId.value, projectUpdated)
          .returning(Monad[DBResult].unit)
        val result = service.projectUpdated(projectId, projectUpdated).transact(tnx).unsafeRunSync()

        result should be(())
      }
    }

    describe("projectDeleted should") {
      it("update project and tasks in the read model") {
        val projectReadSideRepository    = mock[ProjectReadSideRepository]
        val taskReadSideRepository       = mock[TaskReadSideRepository]
        val statisticsReadSideRepository = mock[StatisticsReadSideRepository]
        val service                      =
          new ReadSideServiceLive(projectReadSideRepository, taskReadSideRepository, statisticsReadSideRepository)

        val deletedProject = project.copy(deletedAt = now.some)
        val tasksToDelete  = List[TaskReadSideEntity]().pure[DBResult]

        (projectReadSideRepository.get _)
          .expects(deletedProject.projectId)
          .returning(projectReadModel.some.pure[DBResult])
        (projectReadSideRepository.deleteProject _)
          .expects(projectReadModel.dbId, projectReadModel.projectId, now)
          .returning(().pure[DBResult])
        (taskReadSideRepository.getNotDeleted _)
          .expects(projectReadModel.dbId)
          .returning(tasksToDelete)
        (taskReadSideRepository.deleteAll _).expects(projectReadModel.dbId, now).returning(Monad[DBResult].unit)

        val result = service.projectDeleted(deletedProject).transact(tnx).unsafeRunSync()

        result should be(())
      }
      it("update statistics in the read model") {
        val projectReadSideRepository    = mock[ProjectReadSideRepository]
        val taskReadSideRepository       = mock[TaskReadSideRepository]
        val statisticsReadSideRepository = mock[StatisticsReadSideRepository]
        val service                      =
          new ReadSideServiceLive(projectReadSideRepository, taskReadSideRepository, statisticsReadSideRepository)

        val deletedProject = project.copy(deletedAt = now.some)
        val tasksToDelete  = List[TaskReadSideEntity](
          taskReadModel,
          taskReadModel.copy(dbId = 112, duration = Duration.ofMinutes(15), volume = None)
        ).pure[DBResult]

        (projectReadSideRepository.get _)
          .expects(deletedProject.projectId)
          .returning(projectReadModel.some.pure[DBResult])
        (projectReadSideRepository.deleteProject _)
          .expects(projectReadModel.dbId, projectReadModel.projectId, now)
          .returning(().pure[DBResult])
        (taskReadSideRepository.getNotDeleted _)
          .expects(projectReadModel.dbId)
          .returning(tasksToDelete)
        (taskReadSideRepository.deleteAll _).expects(projectReadModel.dbId, now).returning(Monad[DBResult].unit)

        val stats1 = createStatistics(
          numberOfTasks = 10,
          numberOfTasksWithVolume = 5.some,
          durationSum = Duration.ofMinutes(480),
          volumeWeightedTaskDurationSum = Duration.ofMinutes(885).some,
          volumeSum = 23L.some
        )
        (statisticsReadSideRepository.get _)
          .expects(ownerId, taskStartedAtYearMonth)
          .returning(Option(stats1).pure[DBResult])
        val stats2 = createStatistics(
          numberOfTasks = 9,
          numberOfTasksWithVolume = 4.some,
          durationSum = Duration.ofMinutes(450),
          volumeWeightedTaskDurationSum = Duration.ofMinutes(585).some,
          volumeSum = 13L.some
        )
        (statisticsReadSideRepository.get _)
          .expects(ownerId, taskStartedAtYearMonth)
          .returning(Option(stats2).pure[DBResult])
        val stats3 = createStatistics(
          numberOfTasks = 8,
          numberOfTasksWithVolume = 4.some,
          durationSum = Duration.ofMinutes(435),
          volumeWeightedTaskDurationSum = Duration.ofMinutes(585).some,
          volumeSum = 13L.some
        )

        val argCaptor = CaptureAll[StatisticsReadSideEntity]()
        statisticsReadSideRepository.upsert _ expects capture(argCaptor) repeat 2 returning ().pure[DBResult]
        service.projectDeleted(deletedProject).transact(tnx).unsafeRunSync()

        argCaptor.values should matchTo(Seq(stats2, stats3))
      }
    }

    describe("newTask should") {
      it("update project and task in the read model") {
        val (service, statisticsReadSideRepository) = taskAddedPrepare()
        (statisticsReadSideRepository.get _)
          .expects(ownerId, taskStartedAtYearMonth)
          .returning(Option.empty[StatisticsReadSideEntity].pure[DBResult])
        (statisticsReadSideRepository.upsert _).expects(*).returning(().pure[DBResult])

        val result = service.taskAdded(task).transact(tnx).unsafeRunSync()

        result should be(taskReadModel)
      }
      it("create initial statistics") {
        val (service, statisticsReadSideRepository) = taskAddedPrepare()
        (statisticsReadSideRepository.get _)
          .expects(ownerId, taskStartedAtYearMonth)
          .returning(Option.empty[StatisticsReadSideEntity].pure[DBResult])
        val stats                                   = createStatistics(
          numberOfTasks = 1,
          numberOfTasksWithVolume = 1.some,
          durationSum = taskDuration,
          volumeWeightedTaskDurationSum = taskVolumeWeightedDuration.some,
          volumeSum = taskVolume.longValue.some
        )
        // todo arg capture
        (statisticsReadSideRepository.upsert _).expects(stats).returning(().pure[DBResult])

        service.taskAdded(task).transact(tnx).unsafeRunSync()
      }
      it("update existing statistics") {
        val (service, statisticsReadSideRepository) = taskAddedPrepare()
        val currentStats                            = createStatistics(
          numberOfTasks = 10,
          numberOfTasksWithVolume = 5.some,
          durationSum = Duration.ofMinutes(480),
          volumeWeightedTaskDurationSum = Duration.ofMinutes(885).some,
          volumeSum = 23L.some
        )
        (statisticsReadSideRepository.get _)
          .expects(ownerId, taskStartedAtYearMonth)
          .returning(Option(currentStats).pure[DBResult])

        val newStats  = createStatistics(
          numberOfTasks = 11,
          numberOfTasksWithVolume = 6.some,
          durationSum = Duration.ofMinutes(510),
          volumeWeightedTaskDurationSum = Duration.ofMinutes(1185).some,
          volumeSum = 33L.some
        )
        val argCaptor = CaptureOne[StatisticsReadSideEntity]()
        statisticsReadSideRepository.upsert _ expects capture(argCaptor) returning ().pure[DBResult]

        service.taskAdded(task).transact(tnx).unsafeRunSync()
        argCaptor.value should matchTo(newStats)
      }
      it("create initial statistics for two months") {
        val lateTask                                = task.copy(
          startedAt = LocalDateTime.parse("2020-08-31T23:00:00"),
          duration = Duration.ofHours(2),
          volume = None,
          comment = None
        )
        val (service, statisticsReadSideRepository) = taskAddedPrepare(lateTask, TaskReadSideEntity(lateTask))
        (statisticsReadSideRepository.get _)
          .expects(ownerId, YearMonth.of(2020, 8))
          .returning(Option.empty[StatisticsReadSideEntity].pure[DBResult])
        (statisticsReadSideRepository.get _)
          .expects(ownerId, YearMonth.of(2020, 9))
          .returning(Option.empty[StatisticsReadSideEntity].pure[DBResult])
        val argCaptor                               = CaptureAll[StatisticsReadSideEntity]()
        statisticsReadSideRepository.upsert _ expects capture(argCaptor) repeat 2 returning ().pure[DBResult]

        service.taskAdded(lateTask).transact(tnx).unsafeRunSync()

        val stats8 = createStatistics(
          year = 2020,
          month = 8,
          numberOfTasks = 1,
          numberOfTasksWithVolume = None,
          durationSum = Duration.ofHours(1)
        )
        val stats9 = createStatistics(
          year = 2020,
          month = 9,
          numberOfTasks = 1,
          numberOfTasksWithVolume = None,
          durationSum = Duration.ofHours(1)
        )
        argCaptor.values should matchTo(Seq(stats8, stats9))
      }
    }

    describe("taskDeleted should") {
      it("update project and task in the read model") {
        val projectReadSideRepository    = mock[ProjectReadSideRepository]
        val taskReadSideRepository       = mock[TaskReadSideRepository]
        val statisticsReadSideRepository = mock[StatisticsReadSideRepository]
        val service                      =
          new ReadSideServiceLive(projectReadSideRepository, taskReadSideRepository, statisticsReadSideRepository)
        val now                          = LocalDateTime.now()
        val taskDeleted                  = task.copy(deletedAt = now.some)

        (taskReadSideRepository.get _).expects(taskReadModel.taskId).returning(taskReadModel.some.pure[DBResult])
        (taskReadSideRepository.delete _).expects(taskReadModel.dbId, now).returning(Monad[DBResult].unit)
        (projectReadSideRepository.subtractDuration _)
          .expects(taskReadModel.projectDbId, taskReadModel.duration)
          .returning(().pure[DBResult])
        val currentStats = createStatistics(
          numberOfTasks = 10,
          numberOfTasksWithVolume = 5.some,
          durationSum = Duration.ofMinutes(480),
          volumeWeightedTaskDurationSum = Duration.ofMinutes(885).some,
          volumeSum = 23L.some
        )
        (statisticsReadSideRepository.get _)
          .expects(ownerId, taskStartedAtYearMonth)
          .returning(Option(currentStats).pure[DBResult])
        (statisticsReadSideRepository.upsert _).expects(*).returning(().pure[DBResult])

        val result = service.taskDeleted(taskDeleted).transact(tnx).unsafeRunSync()

        result should be(())
      }
      it("update statistics") {
        val projectReadSideRepository    = mock[ProjectReadSideRepository]
        val taskReadSideRepository       = mock[TaskReadSideRepository]
        val statisticsReadSideRepository = mock[StatisticsReadSideRepository]
        val service                      =
          new ReadSideServiceLive(projectReadSideRepository, taskReadSideRepository, statisticsReadSideRepository)
        val now                          = LocalDateTime.now()
        val taskDeleted                  = task.copy(deletedAt = now.some)

        (taskReadSideRepository.get _).expects(taskReadModel.taskId).returning(taskReadModel.some.pure[DBResult])
        (taskReadSideRepository.delete _).expects(taskReadModel.dbId, now).returning(Monad[DBResult].unit)
        (projectReadSideRepository.subtractDuration _)
          .expects(taskReadModel.projectDbId, taskReadModel.duration)
          .returning(().pure[DBResult])
        val currentStats = createStatistics(
          numberOfTasks = 10,
          numberOfTasksWithVolume = 5.some,
          durationSum = Duration.ofMinutes(480),
          volumeWeightedTaskDurationSum = Duration.ofMinutes(885).some,
          volumeSum = 23L.some
        )
        (statisticsReadSideRepository.get _)
          .expects(ownerId, taskStartedAtYearMonth)
          .returning(Option(currentStats).pure[DBResult])
        val argCaptor    = CaptureOne[StatisticsReadSideEntity]()
        statisticsReadSideRepository.upsert _ expects capture(argCaptor) returning ().pure[DBResult]

        service.taskDeleted(taskDeleted).transact(tnx).unsafeRunSync()

        val newStats = createStatistics(
          numberOfTasks = 9,
          numberOfTasksWithVolume = 4.some,
          durationSum = Duration.ofMinutes(450),
          volumeWeightedTaskDurationSum = Duration.ofMinutes(585).some,
          volumeSum = 13L.some
        )
        argCaptor.value should matchTo(newStats)
      }
      it("update statistics when last task is deleted") {
        val projectReadSideRepository    = mock[ProjectReadSideRepository]
        val taskReadSideRepository       = mock[TaskReadSideRepository]
        val statisticsReadSideRepository = mock[StatisticsReadSideRepository]
        val service                      =
          new ReadSideServiceLive(projectReadSideRepository, taskReadSideRepository, statisticsReadSideRepository)
        val now                          = LocalDateTime.now()
        val taskDeleted                  = task.copy(deletedAt = now.some)

        (taskReadSideRepository.get _).expects(taskReadModel.taskId).returning(taskReadModel.some.pure[DBResult])
        (taskReadSideRepository.delete _).expects(taskReadModel.dbId, now).returning(Monad[DBResult].unit)
        (projectReadSideRepository.subtractDuration _)
          .expects(taskReadModel.projectDbId, taskReadModel.duration)
          .returning(().pure[DBResult])
        val stats     = createStatistics(
          numberOfTasks = 1,
          numberOfTasksWithVolume = 1.some,
          durationSum = taskDuration,
          volumeWeightedTaskDurationSum = taskVolumeWeightedDuration.some,
          volumeSum = taskVolume.longValue.some
        )
        (statisticsReadSideRepository.get _)
          .expects(ownerId, taskStartedAtYearMonth)
          .returning(Option(stats).pure[DBResult])
        val argCaptor = CaptureOne[StatisticsReadSideEntity]()
        statisticsReadSideRepository.upsert _ expects capture(argCaptor) returning ().pure[DBResult]

        service.taskDeleted(taskDeleted).transact(tnx).unsafeRunSync()

        val newStats = createStatistics(
          numberOfTasks = 0,
          numberOfTasksWithVolume = None,
          durationSum = Duration.ofMinutes(0),
          volumeWeightedTaskDurationSum = None,
          volumeSum = None
        )
        argCaptor.value should matchTo(newStats)
      }
    }
  }

  private def taskAddedPrepare(task: TaskEntity = task, taskReadModel: TaskReadSideEntity = taskReadModel) = {
    val projectReadSideRepository    = mock[ProjectReadSideRepository]
    val taskReadSideRepository       = mock[TaskReadSideRepository]
    val statisticsReadSideRepository = mock[StatisticsReadSideRepository]
    val service                      =
      new ReadSideServiceLive(projectReadSideRepository, taskReadSideRepository, statisticsReadSideRepository)

    (taskReadSideRepository.add _).expects(task).returning(taskReadModel.pure[DBResult])
    (projectReadSideRepository.addDuration _)
      .expects(taskReadModel.projectDbId, taskReadModel.duration)
      .returning(().pure[DBResult])

    (service, statisticsReadSideRepository)
  }

  private def createStatistics(
    numberOfTasks: Int,
    numberOfTasksWithVolume: Option[Int],
    durationSum: Duration,
    volumeWeightedTaskDurationSum: Option[Duration] = None,
    volumeSum: Option[Long] = None,
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
