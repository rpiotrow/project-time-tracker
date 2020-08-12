package io.github.rpiotrow.ptt.write.repository

import java.time.{Clock, LocalDateTime}
import java.util.UUID

import cats.effect.IO
import cats.implicits._
import com.softwaremill.diffx.scalatest.DiffMatcher._
import com.softwaremill.diffx.{Derived, Diff}
import doobie.Transactor
import doobie.implicits._
import io.github.rpiotrow.ptt.write.entity.ProjectEntity
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

trait ProjectRepositorySpec { this: AnyFunSpec with should.Matchers =>

  def tnx: Transactor[IO]
  def clock: Clock

  def now: LocalDateTime
  def owner1Id: UUID

  def projectRepo: ProjectRepository

  describe("ProjectRepository") {
    describe("create should") {
      it("return entity") {
        val projectId = "project1"
        val entity    = projectRepo.create(projectId, owner1Id).transact(tnx).unsafeRunSync()

        entity should matchTo(expected(projectId))
      }
      it("write entity that is possible to find by dbId") {
        val projectId = "project2"
        val entity    = projectRepo.create(projectId, owner1Id).transact(tnx).unsafeRunSync()

        readProjectByDbId(entity.dbId) should matchTo(expected(projectId).some)
      }
      it("write entity that is possible to find by projectId") {
        val projectId = "project3"
        projectRepo.create(projectId, owner1Id).transact(tnx).unsafeRunSync()

        readProjectByProjectId(projectId) should matchTo(expected(projectId).some)
      }
    }
    describe("get should") {
      it("return some existing entity") {
        val projectId = "project4"
        projectRepo.create(projectId, owner1Id).transact(tnx).unsafeRunSync()

        val entity = projectRepo.get("project4").transact(tnx).unsafeRunSync()

        entity should matchTo(expected(projectId).some)
      }
      it("return none for non-existing entity") {
        val projectId = "non-existing-project"
        val entity    = projectRepo.get(projectId).transact(tnx).unsafeRunSync()

        entity should be(None)
      }
    }
  }

  private val projects = liveContext.quote { liveContext.querySchema[ProjectEntity]("ptt.projects") }

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

  implicit private val ignoreDbId: Diff[ProjectEntity]   =
    Derived[Diff[ProjectEntity]].ignore[ProjectEntity, Long](_.dbId)
  private def expected(projectId: String): ProjectEntity =
    ProjectEntity(projectId = projectId, createdAt = now, updatedAt = now, deletedAt = None, owner = owner1Id)

}
