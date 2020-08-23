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
         |
         |INSERT INTO ptt_read_model.projects(db_id, project_id, created_at, updated_at, deleted_at, owner, duration_sum)
         |  VALUES (201, 'projectT2', '2020-08-17 10:00:00', '2020-08-17 16:00:00', NULL, '181ab738-1cb7-4adc-a3d6-24e0bdcf1ebf', 21600)
         |;
         |INSERT INTO ptt_read_model.tasks(db_id, task_id, project_db_id, deleted_at, owner, started_at, duration, volume, comment)
         |  VALUES (203, 'dd63a5db-1a08-4fea-907b-6b23dd06c971', 201, NULL, '92d57572-3bee-44f2-b3cc-298e267c8ab6', '2020-08-15T08:00', 5*60*60, 2, 't2-p1'),
         |    (204, '7e791302-e68b-4f5a-9cb5-d4e7e5d74220', 201, '2020-08-20T08:00', '92d57572-3bee-44f2-b3cc-298e267c8ab6', '2020-08-16T08:00', 6*60*60, NULL, 't2-deleted'),
         |    (205, '122099dc-cdb1-4b69-8ffb-c4e67bb9d828', 201, NULL, '92d57572-3bee-44f2-b3cc-298e267c8ab6', '2020-08-17T08:00', 7*60*60, 1, 't2-p3')
         |;
         |INSERT INTO ptt_read_model.projects(db_id, project_id, created_at, updated_at, deleted_at, owner, duration_sum)
         |  VALUES (202, 'projectT3', '2020-08-17 10:00:00', '2020-08-17 16:00:00', NULL, '181ab738-1cb7-4adc-a3d6-24e0bdcf1ebf', 21600)
         |;
         |INSERT INTO ptt_read_model.tasks(db_id, task_id, project_db_id, deleted_at, owner, started_at, duration, volume, comment)
         |  VALUES (206, '2b7be86b-ea79-44c2-b02f-ab7d99f05b29', 202, NULL, '92d57572-3bee-44f2-b3cc-298e267c8ab6', '2020-08-15T08:00', 5*60*60, 2, 't3-p1'),
         |    (207, '032bc426-241e-4fea-9e7b-39a17db36628', 202, '2020-08-20T08:00', '92d57572-3bee-44f2-b3cc-298e267c8ab6', '2020-08-16T08:00', 6*60*60, NULL, 't3-deleted'),
         |    (208, 'a2e3ffde-b359-4826-b265-f887adf6720b', 202, NULL, '92d57572-3bee-44f2-b3cc-298e267c8ab6', '2020-08-17T08:00', 7*60*60, 1, 't3-p3')
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
  private val t2p1                         = TaskReadSideEntity(
    dbId = 203,
    taskId = UUID.fromString("dd63a5db-1a08-4fea-907b-6b23dd06c971"),
    projectDbId = 201,
    deletedAt = None,
    owner = taskOwner,
    startedAt = LocalDateTime.parse("2020-08-15T08:00"),
    duration = Duration.ofHours(5),
    volume = 2.some,
    comment = Some("t2-p1")
  )
  private val t2p3                         = TaskReadSideEntity(
    dbId = 205,
    taskId = UUID.fromString("122099dc-cdb1-4b69-8ffb-c4e67bb9d828"),
    projectDbId = 201,
    deletedAt = None,
    owner = taskOwner,
    startedAt = LocalDateTime.parse("2020-08-17T08:00"),
    duration = Duration.ofHours(7),
    volume = 1.some,
    comment = Some("t2-p3")
  )
  private val t3p1                         = t2p1.copy(
    dbId = 206,
    taskId = UUID.fromString("2b7be86b-ea79-44c2-b02f-ab7d99f05b29"),
    projectDbId = 202,
    comment = Some("t3-p1")
  )
  private val t3p2                         = TaskReadSideEntity(
    dbId = 207,
    taskId = UUID.fromString("032bc426-241e-4fea-9e7b-39a17db36628"),
    projectDbId = 202,
    deletedAt = LocalDateTime.parse("2020-08-20T08:00").some,
    owner = taskOwner,
    startedAt = LocalDateTime.parse("2020-08-16T08:00"),
    duration = Duration.ofHours(6),
    volume = None,
    comment = Some("t3-deleted")
  )
  private val t3p3                         = t2p3.copy(
    dbId = 208,
    taskId = UUID.fromString("a2e3ffde-b359-4826-b265-f887adf6720b"),
    projectDbId = 202,
    comment = Some("t3-p3")
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

    describe("getNotDeleted should") {
      it("return not deleted task for specified project") {
        val list = taskReadSideRepo.getNotDeleted(201).transact(tnx).unsafeRunSync()

        list should matchTo(List(t2p1, t2p3))
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

    describe("deleteAll should") {
      it("soft delete all project tasks in the read model") {
        val now = LocalDateTime.now()
        taskReadSideRepo.deleteAll(202, now).transact(tnx).unsafeRunSync()

        val optionTask1 = taskReadSideRepo.get(t3p1.taskId).transact(tnx).unsafeRunSync()
        val optionTask2 = taskReadSideRepo.get(t3p2.taskId).transact(tnx).unsafeRunSync()
        val optionTask3 = taskReadSideRepo.get(t3p3.taskId).transact(tnx).unsafeRunSync()
        List(optionTask1, optionTask2, optionTask3) should matchTo(
          List(t3p1.copy(deletedAt = now.some).some, t3p2.some, t3p3.copy(deletedAt = now.some).some)
        )
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
