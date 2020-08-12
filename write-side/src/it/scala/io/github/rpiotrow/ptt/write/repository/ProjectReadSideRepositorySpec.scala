package io.github.rpiotrow.ptt.write.repository

import java.time.{Duration, LocalDateTime}
import java.util.UUID

import cats.effect.IO
import cats.implicits._
import com.softwaremill.diffx.scalatest.DiffMatcher._
import com.softwaremill.diffx.{Derived, Diff}
import doobie.Transactor
import doobie.implicits._
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

trait ProjectReadSideRepositorySpec { this: AnyFunSpec with should.Matchers =>

  def tnx: Transactor[IO]

  def owner1Id: UUID

  def projectReadSideRepo: ProjectReadSideRepository

  private val writeSideNow                                         = LocalDateTime.now()
  private def entity(dbId: Long, projectId: String): ProjectEntity =
    ProjectEntity(
      dbId = dbId,
      projectId = projectId,
      createdAt = writeSideNow,
      updatedAt = writeSideNow,
      deletedAt = None,
      owner = owner1Id
    )

  implicit private val ignoreDbId: Diff[ProjectReadSideEntity]   =
    Derived[Diff[ProjectReadSideEntity]].ignore[ProjectReadSideEntity, Long](_.dbId)
  private def expected(projectId: String): ProjectReadSideEntity =
    ProjectReadSideEntity(
      dbId = 0,
      projectId = projectId,
      createdAt = writeSideNow,
      updatedAt = writeSideNow,
      deletedAt = None,
      owner = owner1Id,
      durationSum = Duration.ZERO
    )

  describe("ProjectReadSideRepository") {
    describe("newProject should") {
      it("return entity") {
        val projectId      = "project1"
        val readSideEntity = projectReadSideRepo.newProject(entity(2, projectId)).transact(tnx).unsafeRunSync()

        readSideEntity should matchTo(expected(projectId))
      }
      it("write entity that is possible to find by dbId") {
        val projectId      = "project2"
        val readSideEntity = projectReadSideRepo.newProject(entity(4, projectId)).transact(tnx).unsafeRunSync()

        readProjectByDbId(readSideEntity.dbId) should matchTo(expected(projectId).some)
      }
      it("write entity that is possible to find by projectId") {
        val projectId = "project3"
        projectReadSideRepo.newProject(entity(7, projectId)).transact(tnx).unsafeRunSync()

        readProjectByProjectId(projectId) should matchTo(expected(projectId).some)
      }
    }
  }

  private val projects = liveContext.quote { liveContext.querySchema[ProjectReadSideEntity]("ptt_read_model.projects") }

  private def readProjectByDbId(dbId: Long) = {
    import liveContext._
    liveContext
      .run(liveContext.quote { projects.filter(_.dbId == lift(dbId)) })
      .map(_.headOption)
      .transact(tnx)
      .unsafeRunSync()
  }

  private def readProjectByProjectId(projectId: String) = {
    import liveContext._
    liveContext
      .run(liveContext.quote { projects.filter(_.projectId == lift(projectId)) })
      .map(_.headOption)
      .transact(tnx)
      .unsafeRunSync()
  }

}
