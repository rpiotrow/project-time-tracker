package io.github.rpiotrow.ptt.write.repository

import java.time.{Duration, LocalDateTime}
import java.util.UUID

import cats.effect.IO
import cats.implicits._
import com.softwaremill.diffx.scalatest.DiffMatcher.matchTo
import com.softwaremill.diffx.{Derived, Diff}
import doobie.Transactor
import doobie.implicits._
import io.github.rpiotrow.ptt.write.entity.{TaskEntity, TaskReadSideEntity}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

trait TaskReadSideRepositorySpec { this: AnyFunSpec with should.Matchers =>

  protected def tnx: Transactor[IO]
  protected val clockNow: LocalDateTime
  protected def taskReadSideRepo: TaskReadSideRepository

  protected val taskReadSideRepositoryData =
    sql"""
         |INSERT INTO ptt_read_model.projects(db_id, project_id, created_at, updated_at, deleted_at, owner, duration_sum)
         |  VALUES (200, 'projectT1', '2020-08-17 10:00:00', '2020-08-17 16:00:00', NULL, '181ab738-1cb7-4adc-a3d6-24e0bdcf1ebf', 21600)
         |;
         |INSERT INTO ptt_read_model.tasks(db_id, task_id, project_db_id, deleted_at, owner, started_at, duration, volume, comment)
         |  VALUES (201, 'fcedfe83-42c5-45ee-9ed0-80a70c9b0231', 200, NULL, '92d57572-3bee-44f2-b3cc-298e267c8ab6', '2020-08-20T08:00', 5*60*60, NULL, 'p1'),
         |    (202, '6f2a01a1-a66d-4127-844b-f95bee3d1ace', 200, NULL, '92d57572-3bee-44f2-b3cc-298e267c8ab6', '2020-08-20T08:00', 5*60*60, NULL, 'to-delete')
         |;
         |""".stripMargin
  private val taskOwner: UUID              = UUID.fromString("92d57572-3bee-44f2-b3cc-298e267c8ab6")
  private val readSideTask1                = TaskReadSideEntity(
    dbId = 201,
    taskId = UUID.fromString("fcedfe83-42c5-45ee-9ed0-80a70c9b0231"),
    projectDbId = 200,
    deletedAt = None,
    owner = taskOwner,
    startedAt = LocalDateTime.parse("2020-08-20T08:00"),
    duration = Duration.ofHours(5),
    volume = None,
    comment = Some("p1")
  )
  private val toDelete                     =
    readSideTask1.copy(
      dbId = 202,
      taskId = UUID.fromString("6f2a01a1-a66d-4127-844b-f95bee3d1ace"),
      comment = "to-delete".some
    )

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

    describe("get should") {
      it("return existing") {
        val optionTask = taskReadSideRepo.get(readSideTask1.taskId).transact(tnx).unsafeRunSync()

        optionTask should matchTo(readSideTask1.some)
      }
      it("return None when task with given taskId does not exist") {
        val optionTask = taskReadSideRepo.get(UUID.randomUUID()).transact(tnx).unsafeRunSync()

        optionTask should be(None)
      }
    }

    describe("delete should") {
      it("soft delete task on read side") {
        val now = LocalDateTime.now()
        taskReadSideRepo.delete(toDelete.dbId, now).transact(tnx).unsafeRunSync()

        val optionTask = taskReadSideRepo.get(toDelete.taskId).transact(tnx).unsafeRunSync()
        optionTask should matchTo(toDelete.copy(deletedAt = now.some).some)
      }
    }
  }

  private val taskStart                                     = LocalDateTime.now()
  private def task(): TaskEntity                            =
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
  implicit private val ignoreDbId: Diff[TaskReadSideEntity] =
    Derived[Diff[TaskReadSideEntity]].ignore[TaskReadSideEntity, Long](_.dbId)
  private def taskReadModel(taskId: UUID)                   =
    TaskReadSideEntity(
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

  private val tasks = liveContext.quote { liveContext.querySchema[TaskReadSideEntity]("ptt_read_model.tasks") }

  private def readTaskByDbId(dbId: Long): Option[TaskReadSideEntity] = {
    import liveContext._
    liveContext
      .run(liveContext.quote { tasks.filter(_.dbId == lift(dbId)) })
      .map(_.headOption)
      .transact(tnx)
      .unsafeRunSync()
  }

}
