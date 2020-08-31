package io.github.rpiotrow.ptt.write.service

import java.time.{Duration, LocalDateTime, YearMonth}

import cats.data.OptionT
import cats.implicits._
import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}
import io.github.rpiotrow.ptt.write.entity._
import io.github.rpiotrow.ptt.write.repository._
import org.slf4j.LoggerFactory

trait ReadSideService {
  def projectCreated(created: ProjectEntity): DBResult[Unit]
  def projectUpdated(projectId: ProjectId, updated: ProjectEntity): DBResult[Unit]
  def projectDeleted(deleted: ProjectEntity): DBResult[Unit]

  def taskAdded(projectId: String, added: TaskEntity): DBResult[Unit]
  def taskDeleted(deleted: TaskEntity): DBResult[Unit]
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

  private val logger = LoggerFactory.getLogger("ReadSideService")

  override def projectCreated(created: ProjectEntity): DBResult[Unit] =
    projectReadSideRepository.newProject(created)

  override def projectUpdated(projectId: ProjectId, updated: ProjectEntity): DBResult[Unit] =
    projectReadSideRepository.updateProject(projectId.value, updated)

  override def projectDeleted(deleted: ProjectEntity): DBResult[Unit] =
    (for {
      readModel <- OptionT(projectReadSideRepository.get(deleted.projectId))
      _         <- OptionT(projectDeletedReadModel(readModel, deleted.deletedAt.get))
    } yield ()).getOrElse(logger.warn("Read model update failure: " + "project not found in read model"))

  private def projectDeletedReadModel(readModel: ProjectReadSideEntity, deletedAt: LocalDateTime) =
    for {
      _     <- projectReadSideRepository.deleteProject(readModel.dbId, readModel.projectId, deletedAt)
      tasks <- taskReadSideRepository.getNotDeleted(readModel.dbId)
      _     <- tasks.map(updateStatisticsForDeletedTask).sequence_
      _     <- taskReadSideRepository.deleteAll(readModel.dbId, deletedAt)
    } yield ().some

  override def taskAdded(projectId: String, added: TaskEntity): DBResult[Unit] =
    (for {
      readModel <- OptionT(projectReadSideRepository.get(projectId))
      _         <- OptionT(taskAdded(readModel.dbId, added))
    } yield ()).getOrElse(logger.warn("Read model update failure: " + "project not found in read model"))

  private def taskAdded(projectDbId: Long, added: TaskEntity): DBResult[Option[TaskReadSideEntity]] =
    for {
      readModel <- taskReadSideRepository.add(projectDbId, added)
      _         <- projectReadSideRepository.addDuration(readModel.projectDbId, readModel.duration, added.createdAt)
      _         <- updateStatisticsForAddedTask(readModel)
    } yield readModel.some

  override def taskDeleted(deleted: TaskEntity): DBResult[Unit] =
    (for {
      readModel <- OptionT(taskReadSideRepository.get(deleted.taskId))
      _         <- OptionT(taskDeletedReadModel(readModel, deleted.deletedAt.get))
    } yield ()).getOrElse(logger.warn("Read model update failure: " + "task not found in read model"))

  private def taskDeletedReadModel(readModel: TaskReadSideEntity, deletedAt: LocalDateTime) =
    for {
      _ <- taskReadSideRepository.delete(readModel.dbId, deletedAt)
      _ <- projectReadSideRepository.subtractDuration(readModel.projectDbId, readModel.duration)
      _ <- updateStatisticsForDeletedTask(readModel)
    } yield ().some

  private def updateStatisticsForAddedTask(task: TaskReadSideEntity): DBResult[Unit] =
    updateStatisticsForEachYearMonth(
      task,
      {
        case (yearMonth, duration) => updateStatisticsInDatabase(task.owner, yearMonth, 1, duration, task.volume)
      }
    )

  private def updateStatisticsForDeletedTask(task: TaskReadSideEntity): DBResult[Unit] =
    updateStatisticsForEachYearMonth(
      task,
      {
        case (yearMonth, duration) =>
          updateStatisticsInDatabase(task.owner, yearMonth, -1, duration.negated(), task.volume.map(_ * (-1)))
      }
    )

  private def updateStatisticsForEachYearMonth(
    task: TaskReadSideEntity,
    updateStatisticsInYearMonth: ((YearMonth, Duration)) => DBResult[Unit]
  ) =
    splitDuration(task).map(updateStatisticsInYearMonth).toList.sequence_

  private def splitDuration(task: TaskReadSideEntity): Map[YearMonth, Duration] =
    LocalDateTimeRange(task.startedAt, task.startedAt.plus(task.duration))
      .splitToMonths()
      .map(r => r.fromYearMonth -> r.duration())
      .toMap

  private def updateStatisticsInDatabase(
    user: UserId,
    yearMonth: YearMonth,
    tasksNumber: Int,
    duration: Duration,
    volume: Option[Int]
  ): DBResult[Unit] =
    for {
      current <- statisticsReadSideRepository.get(user, yearMonth)
      createdOrUpdated = initialOrUpdateStatistics(current, user, yearMonth, tasksNumber, duration, volume)
      _ <- statisticsReadSideRepository.upsert(createdOrUpdated)
    } yield ()

  private def initialOrUpdateStatistics(
    current: Option[StatisticsReadSideEntity],
    user: UserId,
    yearMonth: YearMonth,
    tasksNumber: Int,
    duration: Duration,
    volume: Option[Int]
  ): StatisticsReadSideEntity =
    current match {
      case None    => initialStatistics(user, yearMonth, tasksNumber, duration, volume)
      case Some(v) => updateStatistics(v, tasksNumber, duration, volume)
    }

  private def initialStatistics(
    user: UserId,
    yearMonth: YearMonth,
    tasksNumber: Int,
    duration: Duration,
    volume: Option[Int]
  ): StatisticsReadSideEntity =
    StatisticsReadSideEntity(
      dbId = 0,
      owner = user,
      year = yearMonth.getYear,
      month = yearMonth.getMonthValue,
      numberOfTasks = tasksNumber,
      numberOfTasksWithVolume = Option.when(volume.isDefined)(tasksNumber),
      durationSum = duration,
      volumeWeightedTaskDurationSum = volume.map(duration.multipliedBy(_)),
      volumeSum = volume.map(_.longValue)
    )

  private def updateStatistics(
    statistics: StatisticsReadSideEntity,
    tasksNumber: Int,
    duration: Duration,
    volume: Option[Int]
  ): StatisticsReadSideEntity = {
    // tasks number is 1 when task was added and -1 when it was deleted
    val newNumberOfTasks           = statistics.numberOfTasks + tasksNumber
    // we multiply volume by tasksNumber to get negative value task deletion case,
    // since when duration and volume are negative we would get positive value of volume weighted duration
    val volumeWeightedTaskDuration = volume.map(v => duration.multipliedBy(v * tasksNumber))

    statistics.copy(
      numberOfTasks = newNumberOfTasks,
      numberOfTasksWithVolume = (statistics.numberOfTasksWithVolume ++ Option.when(volume.isDefined)(tasksNumber))
        .reduceOption(_ + _)
        .flatMap(n => Option.when(n > 0)(n)),
      durationSum = statistics.durationSum.plus(duration),
      volumeWeightedTaskDurationSum = (statistics.volumeWeightedTaskDurationSum ++ volumeWeightedTaskDuration)
        .reduceOption(_.plus(_))
        .flatMap(d => Option.when(!d.isZero)(d)),
      volumeSum = (statistics.volumeSum ++ volume.map(_.longValue))
        .reduceOption(_ + _)
        .flatMap(n => Option.when(n > 0)(n))
    )
  }

}
