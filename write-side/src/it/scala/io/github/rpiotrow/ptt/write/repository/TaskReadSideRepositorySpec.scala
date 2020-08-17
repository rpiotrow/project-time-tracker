package io.github.rpiotrow.ptt.write.repository

import java.time.{Duration, LocalDateTime}
import java.util.UUID

import cats.effect.IO
import cats.implicits._
import com.softwaremill.diffx.scalatest.DiffMatcher.matchTo
import com.softwaremill.diffx.{Derived, Diff}
import doobie.Transactor
import doobie.implicits._
import io.github.rpiotrow.ptt.write.entity.TaskEntity
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

trait TaskReadSideRepositorySpec { this: AnyFunSpec with should.Matchers =>

  protected def tnx: Transactor[IO]
  protected def taskReadSideRepo: TaskReadSideRepository

  protected val taskReadSideRepositoryData =
    sql"""
         |INSERT INTO ptt_read_model.projects(db_id, project_id, created_at, updated_at, deleted_at, owner, duration_sum)
         |  VALUES (200, 'projectT1', '2020-08-17 10:00:00', '2020-08-17 16:00:00', NULL, '181ab738-1cb7-4adc-a3d6-24e0bdcf1ebf', 21600)
         |;
         |""".stripMargin

  describe("TaskReadSideRepository") {
    describe("add should") {
      it("store task in read model schema") {
        val entity   = task()
        val readSide = taskReadSideRepo.add(entity).transact(tnx).unsafeRunSync()

        readTaskByDbId(readSide.dbId) should matchTo(taskReadModel(entity.taskId).some)
      }
      it("return read side task entity") {
        val entity   = task()
        val readSide = taskReadSideRepo.add(entity).transact(tnx).unsafeRunSync()

        readSide should matchTo(taskReadModel(entity.taskId))
      }
    }
  }

  private val taskOwner                             = UUID.randomUUID()
  private val taskStart                             = LocalDateTime.now()
  private def task(): TaskEntity                    =
    TaskEntity(
      dbId = 0,
      taskId = UUID.randomUUID(),
      projectDbId = 200,
      deletedAt = None,
      owner = taskOwner,
      startedAt = taskStart,
      duration = Duration.ofMinutes(30),
      volume = 10.some,
      comment = "some comment".some
    )
  implicit private val ignoreDbId: Diff[TaskEntity] =
    Derived[Diff[TaskEntity]].ignore[TaskEntity, Long](_.dbId)
  private def taskReadModel(taskId: UUID)           =
    TaskEntity(
      dbId = 0,
      taskId = taskId,
      projectDbId = 200,
      deletedAt = None,
      owner = taskOwner,
      startedAt = taskStart,
      duration = Duration.ofMinutes(30),
      volume = 10.some,
      comment = "some comment".some
    )

  private val tasks = liveContext.quote { liveContext.querySchema[TaskEntity]("ptt_read_model.tasks") }

  private def readTaskByDbId(dbId: Long): Option[TaskEntity] = {
    import liveContext._
    liveContext
      .run(liveContext.quote { tasks.filter(_.dbId == lift(dbId)) })
      .map(_.headOption)
      .transact(tnx)
      .unsafeRunSync()
  }

}
