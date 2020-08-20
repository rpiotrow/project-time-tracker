package io.github.rpiotrow.ptt.write.service

import java.time.{Duration, YearMonth}

import cats.implicits._
import io.github.rpiotrow.ptt.api.model.UserId
import io.github.rpiotrow.ptt.write.entity._
import io.github.rpiotrow.ptt.write.repository._

trait ReadSideService {
  def projectCreated(project: ProjectEntity): DBResult[ProjectReadSideEntity]
  def projectDeleted(project: ProjectEntity): DBResult[Unit]

  def taskAdded(task: TaskEntity): DBResult[TaskReadSideEntity]
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

  override def projectCreated(project: ProjectEntity): DBResult[ProjectReadSideEntity] =
    projectReadSideRepository.newProject(project)

  override def projectDeleted(project: ProjectEntity): DBResult[Unit] = {
    projectReadSideRepository.deleteProject(project)
    // TODO: delete all tasks related to project on the read side
    // TODO: update statistics
  }

  override def taskAdded(task: TaskEntity): DBResult[TaskReadSideEntity] =
    for {
      readModel <- taskReadSideRepository.add(task)
      _         <- projectReadSideRepository.addToProjectDuration(readModel.projectDbId, readModel.duration)
      _         <- updateStatisticsForAddedTask(readModel)
    } yield readModel

  private def updateStatisticsForAddedTask(task: TaskReadSideEntity): DBResult[Unit] = {
    splitDuration(task)
      .map({
        case (yearMonth, duration) => updateStatisticsForAddedDuration(task.owner, yearMonth, duration, task.volume)
      })
      .toList
      .sequence_
  }

  private def splitDuration(task: TaskReadSideEntity): Map[YearMonth, Duration] = {
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
      numberOfTasksWithVolume = Option.when(volume.isDefined)(1),
      durationSum = duration,
      volumeWeightedTaskDurationSum = volume.map(duration.multipliedBy(_)),
      volumeSum = volume.map(_.longValue)
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
      numberOfTasksWithVolume =
        (statistics.numberOfTasksWithVolume ++ Option.when(volume.isDefined)(1)).reduceOption(_ + _),
      durationSum = statistics.durationSum.plus(duration),
      volumeWeightedTaskDurationSum =
        (statistics.volumeWeightedTaskDurationSum ++ volume.map(duration.multipliedBy(_))).reduceOption(_.plus(_)),
      volumeSum = (statistics.volumeSum ++ volume.map(_.longValue)).reduceOption(_ + _)
    )
  }

}
