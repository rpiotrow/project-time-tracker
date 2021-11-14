package io.github.rpiotrow.ptt.write.repository

import java.time.Instant
import java.util.UUID

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits._
import com.softwaremill.diffx.generic.auto._
import com.softwaremill.diffx.scalatest.DiffShouldMatcher._
import com.softwaremill.diffx.{Derived, Diff}
import doobie.Transactor
import doobie.implicits._
import eu.timepit.refined.auto._
import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}
import io.github.rpiotrow.ptt.write.entity.ProjectEntity
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

trait ProjectRepositorySpec { this: AnyFunSpec with should.Matchers =>

  protected def tnx: Transactor[IO]
  protected def clockNow: Instant
  protected def projectRepo: ProjectRepository

  private val owner1Id = UserId(UUID.randomUUID())

  describe("ProjectRepository") {
    describe("create should") {
      it("return entity") {
        val projectId: ProjectId = "project1"
        val entity               = projectRepo.create(projectId, owner1Id).transact(tnx).unsafeRunSync()

        entity shouldMatchTo(expected(projectId))
      }
      it("write entity that is possible to find by dbId") {
        val projectId: ProjectId = "project2"
        val entity               = projectRepo.create(projectId, owner1Id).transact(tnx).unsafeRunSync()

        readProjectByDbId(entity.dbId) shouldMatchTo(expected(projectId).some)
      }
      it("write entity that is possible to find by projectId") {
        val projectId: ProjectId = "project3"
        projectRepo.create(projectId, owner1Id).transact(tnx).unsafeRunSync()

        readProjectByProjectId(projectId) shouldMatchTo(expected(projectId).some)
      }
    }
    describe("get should") {
      it("return some existing entity") {
        val projectId: ProjectId = "project4"
        projectRepo.create(projectId, owner1Id).transact(tnx).unsafeRunSync()

        val entity = projectRepo.get("project4").transact(tnx).unsafeRunSync()

        entity shouldMatchTo(expected(projectId).some)
      }
      it("return none for non-existing entity") {
        val projectId: ProjectId = "non-existing-project"
        val entity               = projectRepo.get(projectId).transact(tnx).unsafeRunSync()

        entity should be(None)
      }
    }
    describe("delete should") {
      it("soft delete existing project") {
        val projectId: ProjectId = "projectD1"
        val entity               = projectRepo.create(projectId, owner1Id).transact(tnx).unsafeRunSync()

        projectRepo.delete(entity).transact(tnx).unsafeRunSync()
        readProjectByDbId(entity.dbId) shouldMatchTo(entity.copy(deletedAt = clockNow.some).some)
      }
      it("return soft deleted entity") {
        val projectId: ProjectId = "projectD2"
        val entity               = projectRepo.create(projectId, owner1Id).transact(tnx).unsafeRunSync()

        val deleted = projectRepo.delete(entity).transact(tnx).unsafeRunSync()
        deleted shouldMatchTo(entity.copy(deletedAt = clockNow.some))
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

  private def readProjectByProjectId(projectId: ProjectId) = {
    import liveContext._
    liveContext
      .run(liveContext.quote { projects.filter(_.projectId == lift(projectId)) })
      .map(_.headOption)
      .transact(tnx)
      .unsafeRunSync()
  }

  implicit private val ignoreDbId: Diff[ProjectEntity]      =
    Derived[Diff[ProjectEntity]].ignore(_.dbId)
  private def expected(projectId: ProjectId): ProjectEntity =
    ProjectEntity(projectId = projectId, createdAt = clockNow, deletedAt = None, owner = owner1Id)

}
