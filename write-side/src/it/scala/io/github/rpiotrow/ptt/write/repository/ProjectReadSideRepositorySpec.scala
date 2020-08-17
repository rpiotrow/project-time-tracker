package io.github.rpiotrow.ptt.write.repository

import java.time.{Duration, LocalDateTime}
import java.util.UUID

import cats.effect.IO
import cats.implicits._
import com.softwaremill.diffx.scalatest.DiffMatcher._
import com.softwaremill.diffx.{Derived, Diff}
import doobie.Transactor
import doobie.implicits._
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity, TaskEntity}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

trait ProjectReadSideRepositorySpec { this: AnyFunSpec with should.Matchers =>

  protected def tnx: Transactor[IO]
  protected def projectReadSideRepo: ProjectReadSideRepository

  protected val projectReadSideRepositoryData =
    sql"""
         |INSERT INTO ptt_read_model.projects(db_id, project_id, created_at, updated_at, deleted_at, owner, duration_sum)
         |  VALUES (100, 'projectD1', '2020-08-16 08:00:00', '2020-08-16 18:00:00', NULL, '41a854e4-4262-4672-a7df-c781f535d6ee', 240),
         |    (101, 'projectD2', '2020-08-16 08:00:00', '2020-08-16 18:00:00', NULL, '41a854e4-4262-4672-a7df-c781f535d6ee', 240),
         |    (102, 'duration-sum-zero', '2020-08-16 08:00:00', '2020-08-16 18:00:00', NULL, '41a854e4-4262-4672-a7df-c781f535d6ee', 0),
         |    (103, 'duration-sum-30', '2020-08-16 08:00:00', '2020-08-16 18:00:00', NULL, '41a854e4-4262-4672-a7df-c781f535d6ee', 1800)
         |;
         |""".stripMargin

  private val projectD1       = ProjectReadSideEntity(
    dbId = 100,
    projectId = "projectD1",
    createdAt = LocalDateTime.parse("2020-08-16T08:00:00"),
    updatedAt = LocalDateTime.parse("2020-08-16T18:00:00"),
    deletedAt = None,
    owner = UUID.fromString("41a854e4-4262-4672-a7df-c781f535d6ee"),
    durationSum = Duration.ofSeconds(240)
  )
  private val durationSumZero = projectD1.copy(dbId = 102, projectId = "duration-sum-zero", durationSum = Duration.ZERO)
  private val durationSum30   =
    projectD1.copy(dbId = 103, projectId = "duration-sum-30", durationSum = Duration.ofMinutes(30))

  describe("ProjectReadSideRepository") {
    describe("newProject should") {
      it("return entity") {
        val projectId      = "project1"
        val readSideEntity = projectReadSideRepo.newProject(project(2, projectId)).transact(tnx).unsafeRunSync()

        readSideEntity should matchTo(projectReadModel(projectId))
      }
      it("write entity that is possible to find by dbId") {
        val projectId      = "project2"
        val readSideEntity = projectReadSideRepo.newProject(project(4, projectId)).transact(tnx).unsafeRunSync()

        readProjectByDbId(readSideEntity.dbId) should matchTo(projectReadModel(projectId).some)
      }
      it("write entity that is possible to find by projectId") {
        val projectId = "project3"
        projectReadSideRepo.newProject(project(7, projectId)).transact(tnx).unsafeRunSync()

        readProjectByProjectId(projectId) should matchTo(projectReadModel(projectId).some)
      }
    }
    describe("deletedProject should") {
      it("soft delete project from read model") {
        val now             = LocalDateTime.now()
        val writeSideEntity = project(projectD1).copy(deletedAt = now.some)

        projectReadSideRepo.deleteProject(writeSideEntity).transact(tnx).unsafeRunSync()
        readProjectByDbId(projectD1.dbId) should matchTo(projectD1.copy(deletedAt = now.some).some)
      }
    }
    describe("addToProjectDuration should") {
      it("add duration of task to durationSum when sum is zero") {
        projectReadSideRepo.addToProjectDuration(taskReadModel(durationSumZero.dbId)).transact(tnx).unsafeRunSync()

        val expected = durationSumZero.copy(durationSum = Duration.ofMinutes(30))
        readProjectByDbId(durationSumZero.dbId) should matchTo(expected.some)
      }
      it("add duration of task to durationSum when sum is positive") {
        projectReadSideRepo.addToProjectDuration(taskReadModel(durationSum30.dbId)).transact(tnx).unsafeRunSync()

        val expected = durationSum30.copy(durationSum = Duration.ofMinutes(60))
        readProjectByDbId(durationSum30.dbId) should matchTo(expected.some)
      }
    }
  }

  private val owner1Id                                                 = UUID.randomUUID()
  private val writeSideNow                                             = LocalDateTime.now()
  private def project(dbId: Long, projectId: String): ProjectEntity    =
    ProjectEntity(
      dbId = dbId,
      projectId = projectId,
      createdAt = writeSideNow,
      updatedAt = writeSideNow,
      deletedAt = None,
      owner = owner1Id
    )
  private def project(readModel: ProjectReadSideEntity): ProjectEntity =
    ProjectEntity(
      dbId = readModel.dbId,
      projectId = readModel.projectId,
      createdAt = readModel.createdAt,
      updatedAt = readModel.updatedAt,
      deletedAt = readModel.deletedAt,
      owner = readModel.owner
    )

  implicit private val ignoreDbId: Diff[ProjectReadSideEntity]           =
    Derived[Diff[ProjectReadSideEntity]].ignore[ProjectReadSideEntity, Long](_.dbId)
  private def projectReadModel(projectId: String): ProjectReadSideEntity =
    ProjectReadSideEntity(
      dbId = 0,
      projectId = projectId,
      createdAt = writeSideNow,
      updatedAt = writeSideNow,
      deletedAt = None,
      owner = owner1Id,
      durationSum = Duration.ZERO
    )

  private def taskReadModel(projectDbId: Long) =
    TaskEntity(
      dbId = 11,
      taskId = UUID.randomUUID(),
      projectDbId = projectDbId,
      deletedAt = None,
      owner = UUID.randomUUID(),
      startedAt = LocalDateTime.now(),
      duration = Duration.ofMinutes(30),
      volume = 10.some,
      comment = None
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

  private def readProjectByProjectId(projectId: String): Option[ProjectReadSideEntity] = {
    import liveContext._
    liveContext
      .run(liveContext.quote { projects.filter(_.projectId == lift(projectId)) })
      .map(_.headOption)
      .transact(tnx)
      .unsafeRunSync()
  }

}
