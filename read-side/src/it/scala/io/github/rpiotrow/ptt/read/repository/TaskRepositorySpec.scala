package io.github.rpiotrow.ptt.read.repository

import java.time.{Duration, LocalDateTime}
import java.util.UUID

import com.softwaremill.diffx.scalatest.DiffMatcher._
import doobie.implicits._
import io.github.rpiotrow.ptt.read.entity.TaskEntity
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

trait TaskRepositorySpec {
  this: AnyFunSpec with should.Matchers =>

  def owner1Id: UUID

  def taskRepo: TaskRepository.Service

  val insertTasks =
    sql"""
         |INSERT INTO ptt_read_model.tasks(project_id, deleted_at, owner, started_at, duration, volume, comment)
         |  VALUES (1, NULL, '41a854e4-4262-4672-a7df-c781f535d6ee', '2020-07-22 17:00:00', '2 hours', NULL, 'first task'),
         |    (1, '2020-07-22 17:10:00', '41a854e4-4262-4672-a7df-c781f535d6ee', '2020-07-22 17:00:00', '2 hours', NULL, 'deleted task'),
         |    (2, NULL, '41a854e4-4262-4672-a7df-c781f535d6ee', '2020-07-23 17:00:00', '2 hours', 8, 'high volume different project')
         |;
         |""".stripMargin

  val project1Id = 1
  val project2Id = 2

  lazy val t1 = TaskEntity(
    projectId = project1Id,
    deletedAt = None,
    owner = owner1Id,
    startedAt = LocalDateTime.of(2020, 7, 22, 17, 0),
    duration = Duration.ofHours(2),
    volume = None,
    comment = Some("first task")
  )
  lazy val t2 = TaskEntity(
    projectId = project1Id,
    deletedAt = Some(LocalDateTime.of(2020, 7, 22, 17, 10)),
    owner = owner1Id,
    startedAt = LocalDateTime.of(2020, 7, 22, 17, 0),
    duration = Duration.ofHours(2),
    volume = None,
    comment = Some("deleted task")
  )
  lazy val t3 = TaskEntity(
    projectId = project2Id,
    deletedAt = None,
    owner = owner1Id,
    startedAt = LocalDateTime.of(2020, 7, 23, 17, 0),
    duration = Duration.ofHours(2),
    volume = Some(8),
    comment = Some("high volume different project")
  )

  describe("TaskRepositorySpec read() should") {
    it("return list of tasks for two projects") {
      val result = zio.Runtime.default.unsafeRun(taskRepo.read(List(project1Id, project2Id)))
      result should matchTo(List(t1, t2, t3))
    }
    it("return list of tasks for one project") {
      val result = zio.Runtime.default.unsafeRun(taskRepo.read(List(project2Id)))
      result should matchTo(List(t3))
    }
    it("return empty list when tasks for given projects does not exists") {
      val result = zio.Runtime.default.unsafeRun(taskRepo.read(List(666)))
      result should be(List())
    }
  }

}
