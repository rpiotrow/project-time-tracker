package io.github.rpiotrow.ptt.write.repository

import java.time.{Duration, LocalDateTime}
import java.util.UUID

import eu.timepit.refined.auto._
import cats.effect.IO
import cats.implicits._
import com.softwaremill.diffx.scalatest.DiffMatcher._
import com.softwaremill.diffx.{Derived, Diff}
import doobie.Transactor
import doobie.implicits._
import io.github.rpiotrow.ptt.api.model.{ProjectId, TaskId, UserId}
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity, TaskEntity}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

trait ProjectReadSideRepositorySpec { this: AnyFunSpec with should.Matchers =>

  protected def tnx: Transactor[IO]
  protected def projectReadSideRepo: ProjectReadSideRepository

  protected val projectReadSideRepositoryData =
    sql"""
         |INSERT INTO ptt_read_model.projects(db_id, project_id, created_at, last_add_duration_at, deleted_at, owner, duration_sum)
         |  VALUES (100, 'projectD1', '2020-08-16 08:00:00', '2020-08-16 18:00:00', NULL, '41a854e4-4262-4672-a7df-c781f535d6ee', 240),
         |    (101, 'projectD2', '2020-08-16 08:00:00', '2020-08-16 18:00:00', NULL, '41a854e4-4262-4672-a7df-c781f535d6ee', 240),
         |    (102, 'duration-sum-zero', '2020-08-16 08:00:00', '2020-08-16 18:00:00', NULL, '41a854e4-4262-4672-a7df-c781f535d6ee', 0),
         |    (103, 'duration-sum-30', '2020-08-16 08:00:00', '2020-08-16 18:00:00', NULL, '41a854e4-4262-4672-a7df-c781f535d6ee', 1800),
         |    (104, 'duration-sum-60', '2020-08-16 08:00:00', '2020-08-16 18:00:00', NULL, '41a854e4-4262-4672-a7df-c781f535d6ee', 3600),
         |    (106, 'project-get-one', '2020-08-21 08:00:00', '2020-08-21 23:00:00', NULL, '41a854e4-4262-4672-a7df-c781f535d6ee', 7200)
         |;
         |""".stripMargin

  private val projectD1       = ProjectReadSideEntity(
    dbId = 100,
    projectId = "projectD1",
    createdAt = LocalDateTime.parse("2020-08-16T08:00:00"),
    lastAddDurationAt = LocalDateTime.parse("2020-08-16T18:00:00"),
    deletedAt = None,
    owner = UserId("41a854e4-4262-4672-a7df-c781f535d6ee"),
    durationSum = Duration.ofSeconds(240)
  )
  private val durationSumZero = projectD1.copy(dbId = 102, projectId = "duration-sum-zero", durationSum = Duration.ZERO)
  private val durationSum30   =
    projectD1.copy(dbId = 103, projectId = "duration-sum-30", durationSum = Duration.ofMinutes(30))
  private val durationSum60   =
    projectD1.copy(dbId = 104, projectId = "duration-sum-60", durationSum = Duration.ofMinutes(60))
  private val getOne          =
    projectD1.copy(
      dbId = 106,
      projectId = "project-get-one",
      createdAt = LocalDateTime.parse("2020-08-21T08:00:00"),
      lastAddDurationAt = LocalDateTime.parse("2020-08-21T23:00:00"),
      durationSum = Duration.ofHours(2)
    )

  describe("ProjectReadSideRepository") {
    describe("get should") {
      it("return existing project from the read model") {
        val result = projectReadSideRepo.get("project-get-one").transact(tnx).unsafeRunSync()

        result should matchTo(getOne.some)
      }
      it("return none for non-existing project in the read model") {
        val result = projectReadSideRepo.get("project-get-no-existing").transact(tnx).unsafeRunSync()

        result should be(None)
      }
    }

    describe("newProject should") {
      it("return entity") {
        val projectId: ProjectId = "project1"
        val result: Unit         = projectReadSideRepo.newProject(project(2, projectId)).transact(tnx).unsafeRunSync()

        result should be(())
      }
      it("write entity that is possible to find by projectId") {
        val projectId: ProjectId = "project3"
        projectReadSideRepo.newProject(project(7, projectId)).transact(tnx).unsafeRunSync()

        readProjectByProjectId(projectId) should matchTo(projectReadModel(projectId).some)
      }
    }
    describe("deletedProject should") {
      it("soft delete project from read model") {
        val now      = LocalDateTime.now()
        val expected = projectD1.copy(deletedAt = now.some, durationSum = Duration.ZERO)

        projectReadSideRepo.deleteProject(projectD1.dbId, projectD1.projectId, now).transact(tnx).unsafeRunSync()

        readProjectByDbId(projectD1.dbId) should matchTo(expected.some)
      }
    }
    describe("addDuration should") {
      it("update project on the read side when durationSum is zero") {
        val dateTime = LocalDateTime.now()
        projectReadSideRepo
          .addDuration(durationSumZero.dbId, Duration.ofMinutes(30), dateTime)
          .transact(tnx)
          .unsafeRunSync()

        val expected = durationSumZero.copy(durationSum = Duration.ofMinutes(30), lastAddDurationAt = dateTime)
        readProjectByDbId(durationSumZero.dbId) should matchTo(expected.some)
      }
      it("update project on the read side when durationSum is positive") {
        val dateTime = LocalDateTime.now()
        projectReadSideRepo
          .addDuration(durationSum30.dbId, Duration.ofMinutes(30), dateTime)
          .transact(tnx)
          .unsafeRunSync()

        val expected = durationSum30.copy(durationSum = Duration.ofMinutes(60), lastAddDurationAt = dateTime)
        readProjectByDbId(durationSum30.dbId) should matchTo(expected.some)
      }
    }
    describe("subtractDuration should") {
      it("subtract duration of task from durationSum") {
        projectReadSideRepo
          .subtractDuration(durationSum60.dbId, Duration.ofMinutes(45))
          .transact(tnx)
          .unsafeRunSync()

        val expected = durationSum60.copy(durationSum = Duration.ofMinutes(15))
        readProjectByDbId(durationSum60.dbId) should matchTo(expected.some)
      }
    }
  }

  private val owner1Id                                                      = UserId(UUID.randomUUID())
  private val writeSideNow                                                  = LocalDateTime.now()
  private def project(dbId: Long, projectId: ProjectId): ProjectEntity      =
    ProjectEntity(dbId = dbId, projectId = projectId, createdAt = writeSideNow, deletedAt = None, owner = owner1Id)
  implicit private val ignoreDbId: Diff[ProjectReadSideEntity]              =
    Derived[Diff[ProjectReadSideEntity]].ignore[ProjectReadSideEntity, Long](_.dbId)
  private def projectReadModel(projectId: ProjectId): ProjectReadSideEntity =
    ProjectReadSideEntity(
      dbId = 0,
      projectId = projectId,
      createdAt = writeSideNow,
      lastAddDurationAt = writeSideNow,
      deletedAt = None,
      owner = owner1Id,
      durationSum = Duration.ZERO
    )

  private val projects = liveContext.quote { liveContext.querySchema[ProjectReadSideEntity]("ptt_read_model.projects") }

  private def readProjectByDbId(dbId: Long): Option[ProjectReadSideEntity] = {
    import liveContext._
    liveContext
      .run(liveContext.quote { projects.filter(_.dbId == lift(dbId)) })
      .map(_.headOption)
      .transact(tnx)
      .unsafeRunSync()
  }

  private def readProjectByProjectId(projectId: ProjectId): Option[ProjectReadSideEntity] = {
    import liveContext._
    liveContext
      .run(liveContext.quote { projects.filter(_.projectId == lift(projectId)) })
      .map(_.headOption)
      .transact(tnx)
      .unsafeRunSync()
  }

}
