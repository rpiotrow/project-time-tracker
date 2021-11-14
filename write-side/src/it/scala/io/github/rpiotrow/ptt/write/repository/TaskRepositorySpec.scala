package io.github.rpiotrow.ptt.write.repository

import java.time.{Duration, Instant, OffsetDateTime}
import java.util.UUID
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits._
import com.softwaremill.diffx.generic.auto._
import com.softwaremill.diffx.{Derived, Diff}
import doobie.implicits._
import com.softwaremill.diffx.scalatest.DiffMatcher.matchTo
import doobie.Transactor
import doobie.util.fragment.Fragment
import io.github.rpiotrow.ptt.api.input.TaskInput
import io.github.rpiotrow.ptt.api.model.{TaskId, UserId}
import io.github.rpiotrow.ptt.write.entity.TaskEntity
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

trait TaskRepositorySpec { this: AnyFunSpec with should.Matchers =>

  protected def tnx: Transactor[IO]
  protected val clockNow: Instant
  protected def taskRepo: TaskRepository

  protected val taskRepositoryData: Fragment =
    sql"""
         |INSERT INTO ptt.projects(db_id, project_id, created_at, deleted_at, owner)
         |  VALUES (100, 'projectT1', '2020-08-17 10:00:00', NULL, '181ab738-1cb7-4adc-a3d6-24e0bdcf1ebf'),
         |    (101, 'projectToDelete', '2020-08-21 10:00:00', NULL, '181ab738-1cb7-4adc-a3d6-24e0bdcf1ebf')
         |;
         |INSERT INTO ptt.tasks(db_id, task_id, project_db_id, created_at, deleted_at, owner, started_at, duration, volume, comment)
         |  -- p1:  2020-08-01T08:00 - 2020-08-01T13:00
         |  -- p1a: 2020-08-01T12:00 - 2020-08-01T13:00
         |  -- p2:  2020-08-01T14:00 - 2020-08-01T17:00
         |  -- p2a: 2020-08-01T14:00 - 2020-08-01T15:00
         |  VALUES (201, '35631327-4dea-41aa-8f0a-1e6f335eef99', 100, '2020-08-01T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-01T08:00', 5*60*60, NULL, 'p1'),
         |    (202, '9be91953-dc8d-4320-a628-fe5bfd19fb19', 100, '2020-08-01T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-01T12:00', 1*60*60, NULL, 'p1a'),
         |    (203, 'c4eb1046-aec2-47d6-80f5-368df09bbb00', 100, '2020-08-01T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-01T14:00', 3*60*60, NULL, 'p2'),
         |    (204, 'f990af84-b711-485c-9f01-47dc9d830d4e', 100, '2020-08-01T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-01T14:00', 1*60*60, NULL, 'p2a'),
         |  -- p3:  2020-08-02T13:00 - 2020-08-01T14:00
         |    (205, '6158b42a-6cf1-4597-bf68-ad3f834d0633', 100, '2020-08-01T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-02T13:00', 1*60*60, NULL, 'p3'),
         |  -- p4:  2020-08-03T09:00 - 2020-08-01T17:00
         |    (206, 'ce78dda0-3561-439d-b225-fd905a0521b2', 100, '2020-08-01T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-03T09:00', 8*60*60, NULL, 'p4'),
         |  -- p5:  2020-08-04T12:00 - 2020-08-01T15:00
         |    (207, '16d1d978-91c9-446a-9138-8ef8f8f4f415', 100, '2020-08-01T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-04T12:00', 3*60*60, NULL, 'p5'),
         |  -- p6:  2020-08-05T06:00 - 2020-08-01T09:00
         |  -- p7:  2020-08-05T17:00 - 2020-08-01T20:00
         |    (208, '86097917-8093-4bee-ac60-810e93f497af', 100, '2020-08-01T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-05T06:00', 3*60*60, NULL, 'p6'),
         |    (209, 'ba340d89-eb56-4823-a68f-f9e58253c500', 100, '2020-08-01T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-05T17:00', 3*60*60, NULL, 'p7'),
         |  -- p8:  2020-08-06T11:00 - 2020-08-01T12:00
         |  -- p9:  2020-08-06T15:00 - 2020-08-01T16:00
         |    (210, '93118118-a2e9-4cc9-8724-0fba439d9f18', 100, '2020-08-01T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-06T11:00', 1*60*60, NULL, 'p8'),
         |    (211, '12c7a8a7-4bdd-410d-9b8b-01aefeb18ac2', 100, '2020-08-01T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-06T15:00', 1*60*60, NULL, 'p9'),
         |  -- deleted    : 2020-08-07T08:00 - 2020-08-01T13:00
         |  -- not-deleted: 2020-08-07T14:00 - 2020-08-01T17:00
         |    (212, 'd935230d-c712-4cc7-849f-a56817ff9f77', 100, '2020-08-01T01:00', '2020-08-10T08:00', 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-07T08:00', 5*60*60, NULL, 'deleted'),
         |    (213, '36c9eebe-88f9-473d-af6d-3c88d3b22230', 100, '2020-08-01T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-07T14:00', 3*60*60, NULL, 'not-deleted'),
         |  -- owned    : 2020-08-08T08:00 - 2020-08-01T13:00
         |  -- not-owned: 2020-08-08T14:00 - 2020-08-01T17:00
         |    (214, '9b49e3ee-a8b2-4658-a20c-4894beb10c6f', 100, '2020-08-01T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-08T08:00', 5*60*60, NULL, 'owned'),
         |    (215, '830fa9e6-d909-49fa-8f86-019fa9c67ac0', 100, '2020-08-01T01:00', NULL, '3afb912d-b713-4960-805f-81216a981545', '2020-08-08T14:00', 3*60*60, NULL, 'not-owned'),
         |  -- to-delete
         |    (216, '18caffa9-7b3a-493e-a299-a4fba1efd9ae', 100, '2015-02-13T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-10T14:00', 3*60*60, NULL, 'to-delete-1'),
         |    (217, 'd8ce26d3-722c-41ab-86d0-0630f2d51c04', 100, '2015-02-13T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-10T14:00', 3*60*60, NULL, 'to-delete-2'),
         |  -- to-delete-with-project
         |    (218, 'a5f39662-0a01-4fd9-b35b-15f788518228', 101, '2015-02-13T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-15T17:00', 1*60*60, NULL, 'to-delete-with-project-1'),
         |    (219, '6c9d75b6-c7d7-46e5-9186-b496b1a2880c', 101, '2015-02-13T01:00', '2020-08-21T18:00', 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-15T18:00', 1*60*60, NULL, 'to-delete-with-project-2'),
         |    (220, '03610471-816b-4d3b-a321-5ba662954a1a', 101, '2015-02-13T01:00', NULL, 'ae15ea88-06d6-472c-95e4-a7caf366c337', '2020-08-15T19:00', 1*60*60, NULL, 'to-delete-with-project-3')
         |;
         |""".stripMargin

  private val taskOwner: UserId                = UserId("ae15ea88-06d6-472c-95e4-a7caf366c337")
  private val p1: TaskEntity                   = TaskEntity(
    dbId = 201,
    taskId = TaskId("35631327-4dea-41aa-8f0a-1e6f335eef99"),
    projectDbId = 100,
    createdAt = Instant.parse("2020-08-01T01:00:00Z"),
    deletedAt = None,
    owner = taskOwner,
    startedAt = Instant.parse("2020-08-01T08:00:00Z"),
    duration = Duration.ofHours(5),
    volume = None,
    comment = Some("p1")
  )
  private val p1a: TaskEntity                  = TaskEntity(
    dbId = 202,
    taskId = TaskId("9be91953-dc8d-4320-a628-fe5bfd19fb19"),
    projectDbId = 100,
    createdAt = Instant.parse("2020-08-01T01:00:00Z"),
    deletedAt = None,
    owner = taskOwner,
    startedAt = Instant.parse("2020-08-01T12:00:00Z"),
    duration = Duration.ofHours(1),
    volume = None,
    comment = Some("p1a")
  )
  private val p2: TaskEntity                   = TaskEntity(
    dbId = 203,
    taskId = TaskId("9be91953-dc8d-4320-a628-fe5bfd19fb19"),
    projectDbId = 100,
    createdAt = Instant.parse("2020-08-01T01:00:00Z"),
    deletedAt = None,
    owner = taskOwner,
    startedAt = Instant.parse("2020-08-01T14:00:00Z"),
    duration = Duration.ofHours(3),
    volume = None,
    comment = Some("p2")
  )
  private val p2a: TaskEntity                  = TaskEntity(
    dbId = 204,
    taskId = TaskId("f990af84-b711-485c-9f01-47dc9d830d4e"),
    projectDbId = 100,
    createdAt = Instant.parse("2020-08-01T01:00:00Z"),
    deletedAt = None,
    owner = taskOwner,
    startedAt = Instant.parse("2020-08-01T14:00:00Z"),
    duration = Duration.ofHours(1),
    volume = None,
    comment = Some("p2a")
  )
  private val p3: TaskEntity                   = TaskEntity(
    dbId = 205,
    taskId = TaskId("6158b42a-6cf1-4597-bf68-ad3f834d0633"),
    projectDbId = 100,
    createdAt = Instant.parse("2020-08-01T01:00:00Z"),
    deletedAt = None,
    owner = taskOwner,
    startedAt = Instant.parse("2020-08-02T13:00:00Z"),
    duration = Duration.ofHours(1),
    volume = None,
    comment = Some("p3")
  )
  private val p4: TaskEntity                   = TaskEntity(
    dbId = 206,
    taskId = TaskId("ce78dda0-3561-439d-b225-fd905a0521b2"),
    projectDbId = 100,
    createdAt = Instant.parse("2020-08-01T01:00:00Z"),
    deletedAt = None,
    owner = taskOwner,
    startedAt = Instant.parse("2020-08-03T09:00:00Z"),
    duration = Duration.ofHours(8),
    volume = None,
    comment = Some("p4")
  )
  private val p5: TaskEntity                   = TaskEntity(
    dbId = 207,
    taskId = TaskId("16d1d978-91c9-446a-9138-8ef8f8f4f415"),
    projectDbId = 100,
    createdAt = Instant.parse("2020-08-01T01:00:00Z"),
    deletedAt = None,
    owner = taskOwner,
    startedAt = Instant.parse("2020-08-04T12:00:00Z"),
    duration = Duration.ofHours(3),
    volume = None,
    comment = Some("p5")
  )
  private val notDeleted: TaskEntity           = TaskEntity(
    dbId = 213,
    taskId = TaskId("36c9eebe-88f9-473d-af6d-3c88d3b22230"),
    projectDbId = 100,
    createdAt = Instant.parse("2020-08-01T01:00:00Z"),
    deletedAt = None,
    owner = taskOwner,
    startedAt = Instant.parse("2020-08-07T14:00:00Z"),
    duration = Duration.ofHours(3),
    volume = None,
    comment = Some("not-deleted")
  )
  private val owned: TaskEntity                = TaskEntity(
    dbId = 214,
    taskId = TaskId("36c9eebe-88f9-473d-af6d-3c88d3b22230"),
    projectDbId = 100,
    createdAt = Instant.parse("2020-08-01T01:00:00Z"),
    deletedAt = None,
    owner = taskOwner,
    startedAt = Instant.parse("2020-08-08T08:00:00Z"),
    duration = Duration.ofHours(5),
    volume = None,
    comment = Some("owned")
  )
  private val toDelete1: TaskEntity            = TaskEntity(
    dbId = 216,
    taskId = TaskId("18caffa9-7b3a-493e-a299-a4fba1efd9ae"),
    projectDbId = 100,
    createdAt = Instant.parse("2015-02-13T01:00:00Z"),
    deletedAt = None,
    owner = taskOwner,
    startedAt = Instant.parse("2020-08-10T14:00:00Z"),
    duration = Duration.ofHours(3),
    volume = None,
    comment = Some("to-delete-1")
  )
  private val toDelete2: TaskEntity            = TaskEntity(
    dbId = 217,
    taskId = TaskId("d8ce26d3-722c-41ab-86d0-0630f2d51c04"),
    projectDbId = 100,
    createdAt = Instant.parse("2015-02-13T01:00:00Z"),
    deletedAt = None,
    owner = taskOwner,
    startedAt = Instant.parse("2020-08-10T14:00:00Z"),
    duration = Duration.ofHours(3),
    volume = None,
    comment = Some("to-delete-2")
  )
  private val toDeleteWithProject1: TaskEntity = TaskEntity(
    dbId = 218,
    taskId = TaskId("a5f39662-0a01-4fd9-b35b-15f788518228"),
    projectDbId = 101,
    createdAt = Instant.parse("2015-02-13T01:00:00Z"),
    deletedAt = None,
    owner = taskOwner,
    startedAt = Instant.parse("2020-08-15T17:00:00Z"),
    duration = Duration.ofHours(1),
    volume = None,
    comment = Some("to-delete-with-project-1")
  )
  private val toDeleteWithProject2: TaskEntity = TaskEntity(
    dbId = 219,
    taskId = TaskId("6c9d75b6-c7d7-46e5-9186-b496b1a2880c"),
    projectDbId = 101,
    createdAt = Instant.parse("2015-02-13T01:00:00Z"),
    deletedAt = Instant.parse("2020-08-21T18:00:00Z").some,
    owner = taskOwner,
    startedAt = Instant.parse("2020-08-15T18:00:00Z"),
    duration = Duration.ofHours(1),
    volume = None,
    comment = Some("to-delete-with-project-2")
  )
  private val toDeleteWithProject3: TaskEntity = TaskEntity(
    dbId = 220,
    taskId = TaskId("03610471-816b-4d3b-a321-5ba662954a1a"),
    projectDbId = 101,
    createdAt = Instant.parse("2015-02-13T01:00:00Z"),
    deletedAt = None,
    owner = taskOwner,
    startedAt = Instant.parse("2020-08-15T19:00:00Z"),
    duration = Duration.ofHours(1),
    volume = None,
    comment = Some("to-delete-with-project-3")
  )

  describe("TaskRepository") {
    describe("add should") {
      it("return entity") {
        val taskOwner = UserId(UUID.randomUUID())
        val entity    = taskRepo.add(100, taskInput, taskOwner).transact(tnx).unsafeRunSync()

        entity should matchTo(taskEntity(100, taskOwner))
      }
      it("write entity that is possible to find by dbId") {
        val taskOwner = UserId(UUID.randomUUID())
        val entity    = taskRepo.add(100, taskInput, taskOwner).transact(tnx).unsafeRunSync()

        readTaskByDbId(entity.dbId) should matchTo(taskEntity(100, taskOwner).some)
      }
    }

    describe("overlapping should") {
      val newTaskDuration = Duration.ofHours(3)
      describe("for tasks of the same user") {
        it("""arg:-------xxxxxxxx----
       |       db:--p1p1p1p1--p2p2p2-
       |       db:-------p1a--p2a----
       |       return p1,p2,p1a,p2a""".stripMargin) {
          val list = taskRepo
            .overlapping(taskOwner, Instant.parse("2020-08-01T12:00:00Z"), newTaskDuration)
            .transact(tnx)
            .unsafeRunSync()

          list should matchTo(List(p1, p1a, p2, p2a))
        }
        it("""arg:-------xxxxxxxx----
       |       db:----------p3-------
       |       return p3""".stripMargin) {
          val list = taskRepo
            .overlapping(taskOwner, Instant.parse("2020-08-02T12:00:00Z"), newTaskDuration)
            .transact(tnx)
            .unsafeRunSync()

          list should matchTo(List(p3))
        }
        it("""arg:-------xxxxxxxx----
       |       db:-----p4p4p4p4p4p4--
       |       return p4""".stripMargin) {
          val list = taskRepo
            .overlapping(taskOwner, Instant.parse("2020-08-03T12:00:00Z"), newTaskDuration)
            .transact(tnx)
            .unsafeRunSync()

          list should matchTo(List(p4))
        }
        it("""arg:-------xxxxxxxx----
       |       db:-------p5p5p5p5----
       |       return p5""".stripMargin) {
          val list = taskRepo
            .overlapping(taskOwner, Instant.parse("2020-08-04T12:00:00Z"), newTaskDuration)
            .transact(tnx)
            .unsafeRunSync()

          list should matchTo(List(p5))
        }
        it("""arg:-------xxxxxxxx----
       |       db:--p6------------p7-
       |       return empty list""".stripMargin) {
          val list = taskRepo
            .overlapping(taskOwner, Instant.parse("2020-08-05T12:00:00Z"), newTaskDuration)
            .transact(tnx)
            .unsafeRunSync()

          list should be(List.empty)
        }
        it("""arg:-------xxxxxxxx----
       |       db:-----p8--------p9--
       |       return empty list""".stripMargin) {
          val list = taskRepo
            .overlapping(taskOwner, Instant.parse("2020-08-06T12:00:00Z"), newTaskDuration)
            .transact(tnx)
            .unsafeRunSync()

          list should be(List.empty)
        }
        it("should return only not-deleted overlapping tasks") {
          val list = taskRepo
            .overlapping(taskOwner, Instant.parse("2020-08-07T12:00:00Z"), newTaskDuration)
            .transact(tnx)
            .unsafeRunSync()

          list should matchTo(List(notDeleted))
        }
      }
      describe("for tasks of different user") {
        it("return overlapping task owned only by given user ") {
          val list = taskRepo
            .overlapping(taskOwner, Instant.parse("2020-08-08T12:00:00Z"), newTaskDuration)
            .transact(tnx)
            .unsafeRunSync()

          list should matchTo(List(owned))
        }
      }
    }

    describe("get should") {
      it("return existing") {
        val optionTask = taskRepo.get(p1.taskId).transact(tnx).unsafeRunSync()

        optionTask should matchTo(p1.some)
      }
      it("return None when task with given taskId does not exist") {
        val optionTask = taskRepo.get(TaskId.random()).transact(tnx).unsafeRunSync()

        optionTask should be(Option.empty)
      }
    }

    describe("delete should") {
      it("return deleted task") {
        val result = taskRepo.delete(toDelete1).transact(tnx).unsafeRunSync()

        result should matchTo(result.copy(deletedAt = clockNow.some))
      }
      it("soft delete task") {
        taskRepo.delete(toDelete2).transact(tnx).unsafeRunSync()

        val optionTask = taskRepo.get(toDelete2.taskId).transact(tnx).unsafeRunSync()
        optionTask should matchTo(toDelete2.copy(deletedAt = clockNow.some).some)
      }
    }

    describe("deleteAll should") {
      it("soft delete tasks from the project") {
        val now = Instant.now()
        taskRepo.deleteAll(101, now).transact(tnx).unsafeRunSync()

        val optionTask1 = taskRepo.get(toDeleteWithProject1.taskId).transact(tnx).unsafeRunSync()
        val optionTask2 = taskRepo.get(toDeleteWithProject2.taskId).transact(tnx).unsafeRunSync()
        val optionTask3 = taskRepo.get(toDeleteWithProject3.taskId).transact(tnx).unsafeRunSync()
        List(optionTask1, optionTask2, optionTask3) should matchTo(
          List(
            toDeleteWithProject1.copy(deletedAt = now.some).some,
            toDeleteWithProject2.some,
            toDeleteWithProject3.copy(deletedAt = now.some).some
          )
        )
      }
    }
  }

  private val now                                            = OffsetDateTime.now()
  private val taskInput                                      =
    TaskInput(startedAt = now, duration = Duration.ofMinutes(30), volume = 10.some, comment = "text".some)
  implicit private val ignoreDbIdAndTaskId: Diff[TaskEntity] =
    Derived[Diff[TaskEntity]].ignore(_.dbId).ignore(_.taskId)
  private def taskEntity(projectDbId: Long, owner: UserId)   =
    TaskEntity(
      dbId = 0,
      taskId = TaskId.random(),
      projectDbId = projectDbId,
      createdAt = clockNow,
      deletedAt = None,
      owner = owner,
      startedAt = taskInput.startedAt.toInstant,
      duration = taskInput.duration,
      volume = taskInput.volume,
      comment = taskInput.comment
    )

  private val tasks = liveContext.quote { liveContext.querySchema[TaskEntity]("ptt.tasks") }

  private def readTaskByDbId(dbId: Long): Option[TaskEntity] = {
    import liveContext._
    liveContext
      .run(liveContext.quote {
        tasks.filter(_.dbId == lift(dbId))
      })
      .map(_.headOption)
      .transact(tnx)
      .unsafeRunSync()
  }

}
