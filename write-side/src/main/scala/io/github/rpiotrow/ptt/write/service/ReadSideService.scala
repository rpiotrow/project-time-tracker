package io.github.rpiotrow.ptt.write.service

import java.time.{Duration, YearMonth}

import cats.data.EitherT
import cats.implicits._
import io.github.rpiotrow.ptt.api.model.UserId
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity, StatisticsReadSideEntity, TaskEntity}
import io.github.rpiotrow.ptt.write.repository.{
  DBResult,
  ProjectReadSideRepository,
  StatisticsReadSideRepository,
  TaskReadSideRepository
}

trait ReadSideService {
  def projectCreated(project: ProjectEntity): EitherT[DBResult, AppFailure, ProjectReadSideEntity]
  def projectDeleted(project: ProjectEntity): EitherT[DBResult, AppFailure, Unit]

  def taskAdded(task: TaskEntity): EitherT[DBResult, AppFailure, TaskEntity]
}

object ReadSideService {
  val live: ReadSideService = new ReadSideServiceLive(
    ProjectReadSideRepository.live,
    TaskReadSideRepository.live,
    StatisticsReadSideRepository.live
  )
}

private[service] class ReadSideServiceLive(
  private val projectReadSideRepository: ProjectReadSideRepository,
  private val taskReadSideRepository: TaskReadSideRepository,
  private val statisticsReadSideRepository: StatisticsReadSideRepository
) extends ReadSideService {

  override def projectCreated(project: ProjectEntity): EitherT[DBResult, AppFailure, ProjectReadSideEntity] = {
    EitherT.right[AppFailure](projectReadSideRepository.newProject(project))
  }

  override def projectDeleted(project: ProjectEntity): EitherT[DBResult, AppFailure, Unit] = {
    EitherT.right[AppFailure](projectReadSideRepository.deleteProject(project))
    // TODO: delete all tasks related to project on the read side
    // TODO: update statistics
  }

  override def taskAdded(task: TaskEntity): EitherT[DBResult, AppFailure, TaskEntity] = {
    EitherT.right[AppFailure](for {
      readModel <- taskReadSideRepository.add(task)
      _         <- projectReadSideRepository.addToProjectDuration(readModel)
      _         <- updateStatisticsForAddedTask(readModel)
    } yield readModel)
  }

  private def updateStatisticsForAddedTask(task: TaskEntity): DBResult[Unit] = {
    splitDuration(task)
      .map({
        case (yearMonth, duration) => updateStatisticsForAddedDuration(task.owner, yearMonth, duration, task.volume)
      })
      .toList
      .sequence_
  }

  private def splitDuration(task: TaskEntity): Map[YearMonth, Duration] = {
    LocalDateTimeRange(task.startedAt, task.startedAt.plus(task.duration))
      .splitToMonths()
      .map(r => r.fromYearMonth -> r.duration())
      .toMap
  }

  private def updateStatisticsForAddedDuration(
    user: UserId,
    yearMonth: YearMonth,
    duration: Duration,
    volume: Option[Int]
  ): DBResult[Unit] = {
    for {
      current <- statisticsReadSideRepository.get(user, yearMonth)
      createdOrUpdated = createOrUpdateStatisticsForAddedDuration(current, user, yearMonth, duration, volume)
      _ <- statisticsReadSideRepository.upsert(createdOrUpdated)
    } yield ()
  }

  private def createOrUpdateStatisticsForAddedDuration(
    current: Option[StatisticsReadSideEntity],
    user: UserId,
    yearMonth: YearMonth,
    duration: Duration,
    volume: Option[Int]
  ): StatisticsReadSideEntity =
    current match {
      case None    => initialStatistics(user, yearMonth, duration, volume)
      case Some(v) => updateStatisticsForAddedDuration(v, duration, volume)
    }

  private def initialStatistics(
    user: UserId,
    yearMonth: YearMonth,
    duration: Duration,
    volume: Option[Int]
  ): StatisticsReadSideEntity = {
    StatisticsReadSideEntity(
      dbId = 0,
      owner = user,
      year = yearMonth.getYear,
      month = yearMonth.getMonthValue,
      numberOfTasks = 1,
      numberOfTasksWithVolume = if (volume.isDefined) 1 else 0,
      durationSum = duration,
      volumeWeightedTaskDurationSum = volume.map(duration.multipliedBy(_)),
      volumeSum = volume.map(BigDecimal(_))
    )
  }

  private def updateStatisticsForAddedDuration(
    statistics: StatisticsReadSideEntity,
    duration: Duration,
    volume: Option[Int]
  ): StatisticsReadSideEntity = {
    val newNumberOfTasks = statistics.numberOfTasks + 1

    statistics.copy(
      numberOfTasks = newNumberOfTasks,
      numberOfTasksWithVolume = statistics.numberOfTasksWithVolume + (if (volume.isDefined) 1 else 0),
      durationSum = statistics.durationSum.plus(duration),
      volumeWeightedTaskDurationSum =
        (statistics.volumeWeightedTaskDurationSum.toList ++ volume.map(duration.multipliedBy(_)).toList)
          .combineAllOption((sum: Duration, volumeWeighted: Duration) => sum.plus(volumeWeighted)),
      volumeSum = (statistics.volumeSum.toList ++ volume.map(BigDecimal(_)).toList).combineAllOption
    )
  }

}
