package io.github.rpiotrow.ptt.read.repository

import java.time.{Duration, LocalDateTime}
import java.util.UUID

import com.softwaremill.diffx.scalatest.DiffMatcher._
import doobie.implicits._
import eu.timepit.refined.auto._
import io.github.rpiotrow.projecttimetracker.api.param.OrderingDirection.{Ascending, Descending}
import io.github.rpiotrow.projecttimetracker.api.param.ProjectOrderingField.{CreatedAt, UpdatedAt}
import io.github.rpiotrow.ptt.read.entity.ProjectEntity
import io.github.rpiotrow.ptt.read.repository.ProjectRepository.ProjectListSearchParams
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

trait ProjectRepositorySpec { this: AnyFunSpec with should.Matchers =>

  def owner1Id: UUID

  def projectRepo: ProjectRepository.Service

  val insertProjects =
    sql"""
         |INSERT INTO ptt_read_model.projects(db_id, id, created_at, updated_at, deleted_at, owner, duration_sum)
         |  VALUES (1, 'first', '2020-07-22 15:00:00', '2020-07-22 18:00:00', NULL, '41a854e4-4262-4672-a7df-c781f535d6ee', '4 hours'),
         |    (2, 'second', '2020-07-22 15:10:00', '2020-07-22 17:00:00', NULL, '41a854e4-4262-4672-a7df-c781f535d6ee', '0 hours'),
         |    (3, 'deleted', '2020-07-31 15:00:00', '2020-07-31 18:00:00', '2020-07-31 18:00:00', '41a854e4-4262-4672-a7df-c781f535d6ee', '0 hours')
         |;
         |""".stripMargin

  val defaultParams = ProjectListSearchParams(List(), None, None, None, None, None, 0, 25)
  lazy val p1       = ProjectEntity(
    dbId = 1,
    id = "first",
    createdAt = LocalDateTime.of(2020, 7, 22, 15, 0),
    updatedAt = LocalDateTime.of(2020, 7, 22, 18, 0),
    deletedAt = None,
    owner = owner1Id,
    durationSum = Duration.ofHours(4)
  )
  lazy val p2       = ProjectEntity(
    dbId = 2,
    id = "second",
    createdAt = LocalDateTime.of(2020, 7, 22, 15, 10),
    updatedAt = LocalDateTime.of(2020, 7, 22, 17, 0),
    deletedAt = None,
    owner = owner1Id,
    durationSum = Duration.ofHours(0)
  )
  lazy val p3       = ProjectEntity(
    dbId = 3,
    id = "deleted",
    createdAt = LocalDateTime.of(2020, 7, 31, 15, 0),
    updatedAt = LocalDateTime.of(2020, 7, 31, 18, 0),
    deletedAt = Some(LocalDateTime.of(2020, 7, 31, 18, 0)),
    owner = owner1Id,
    durationSum = Duration.ofHours(0)
  )

  describe("ProjectRepositorySpec list() should") {
    it("return list with all projects") {
      val result = zio.Runtime.default.unsafeRunTask(projectRepo.list(defaultParams))
      result should matchTo(List(p1, p2, p3))
    }
    it("return one project based on one id") {
      val result = zio.Runtime.default.unsafeRunTask(projectRepo.list(defaultParams.copy(ids = List("first"))))
      result should matchTo(List(p1))
    }
    it("return two project based on three ids (one of non-existing project)") {
      val result = zio.Runtime.default.unsafeRunTask(
        projectRepo.list(defaultParams.copy(ids = List("first", "non-existing", "deleted")))
      )
      result should matchTo(List(p1, p3))
    }
    it("return empty list for non-existing project id") {
      val result = zio.Runtime.default.unsafeRunTask(projectRepo.list(defaultParams.copy(ids = List("non-existing"))))
      result should be(List())
    }
    it("return projects to createdAt") {
      val result = zio.Runtime.default.unsafeRunTask(
        projectRepo.list(
          defaultParams
            .copy(to = Some(LocalDateTime.of(2020, 7, 25, 15, 0)))
        )
      )
      result should matchTo(List(p1, p2))
    }
    it("return projects from createdAt time range") {
      val result = zio.Runtime.default.unsafeRunTask(
        projectRepo.list(
          defaultParams
            .copy(from = Some(LocalDateTime.of(2020, 7, 15, 15, 0)), to = Some(LocalDateTime.of(2020, 7, 25, 15, 0)))
        )
      )
      result should matchTo(List(p1, p2))
    }
    it("return only deleted projects") {
      val result = zio.Runtime.default.unsafeRunTask(projectRepo.list(defaultParams.copy(deleted = Some(true))))
      result should matchTo(List(p3))
    }
    it("return only not deleted projects") {
      val result = zio.Runtime.default.unsafeRunTask(projectRepo.list(defaultParams.copy(deleted = Some(false))))
      result should matchTo(List(p1, p2))
    }
    it("return order list by createdAt ascending") {
      val result = zio.Runtime.default.unsafeRunTask(projectRepo.list(defaultParams.copy(orderBy = Some(CreatedAt))))
      result should matchTo(List(p1, p2, p3))
    }
    it("return order list by createdAt descending") {
      val result = zio.Runtime.default.unsafeRunTask(
        projectRepo.list(defaultParams.copy(orderBy = Some(CreatedAt), orderDirection = Some(Descending)))
      )
      result should matchTo(List(p3, p2, p1))
    }
    it("return order list by updatedAt ascending") {
      val result = zio.Runtime.default.unsafeRunTask(
        projectRepo.list(defaultParams.copy(orderBy = Some(UpdatedAt), orderDirection = Some(Ascending)))
      )
      result should matchTo(List(p2, p1, p3))
    }
    it("return order list by updatedAt descending") {
      val result = zio.Runtime.default.unsafeRunTask(
        projectRepo.list(defaultParams.copy(orderBy = Some(UpdatedAt), orderDirection = Some(Descending)))
      )
      result should matchTo(List(p3, p1, p2))
    }
    it("return return second page with createdAt descending order") {
      val result = zio.Runtime.default.unsafeRunTask(projectRepo.list(defaultParams.copy(pageNumber = 1, pageSize = 1)))
      result should matchTo(List(p2))
    }
    it("return return empty list when page number is out of scope of pages") {
      val result =
        zio.Runtime.default.unsafeRunTask(projectRepo.list(defaultParams.copy(pageNumber = 10, pageSize = 2)))
      result should be(List())
    }
  }

}