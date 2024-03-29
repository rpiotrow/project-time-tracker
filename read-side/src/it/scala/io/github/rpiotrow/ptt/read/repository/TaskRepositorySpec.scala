package io.github.rpiotrow.ptt.read.repository

import java.time.{Duration, Instant}
import com.softwaremill.diffx.generic.auto._
import com.softwaremill.diffx.scalatest.DiffShouldMatcher._
import doobie.implicits._
import doobie.util.fragment.Fragment
import io.github.rpiotrow.ptt.api.model.{TaskId, UserId}
import io.github.rpiotrow.ptt.read.entity.TaskEntity
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

trait TaskRepositorySpec {
  this: AnyFunSpec with should.Matchers =>

  def owner1Id: UserId

  private val t1Id = TaskId("aa37e5a5-2f5b-46d9-896a-28422df74ff1")
  private val t2Id = TaskId("f445a2cd-6c21-4d10-ae96-2e1b1199c09d")
  private val t3Id = TaskId("5c4c4b21-7ba2-47e7-a0e4-e477231648ee")

  def taskRepo: TaskRepository.Service

  val insertTasks: Fragment =
    sql"""
         |INSERT INTO ptt_read_model.tasks(task_id, project_db_id, deleted_at, owner, started_at, duration, volume, comment)
         |  VALUES ('aa37e5a5-2f5b-46d9-896a-28422df74ff1', 1, NULL, '41a854e4-4262-4672-a7df-c781f535d6ee', '2020-07-22 17:00:00', 7200, NULL, 'first task'),
         |    ('f445a2cd-6c21-4d10-ae96-2e1b1199c09d', 1, '2020-07-22 17:10:00', '41a854e4-4262-4672-a7df-c781f535d6ee', '2020-07-22 17:00:00', 7200, NULL, 'deleted task'),
         |    ('5c4c4b21-7ba2-47e7-a0e4-e477231648ee', 2, NULL, '41a854e4-4262-4672-a7df-c781f535d6ee', '2020-07-23 17:00:00', 7200, 8, 'high volume different project')
         |;
         |""".stripMargin

  val project1Id = 1
  val project2Id = 2

  lazy val t1 = TaskEntity(
    taskId = t1Id,
    projectDbId = project1Id,
    deletedAt = None,
    owner = owner1Id,
    startedAt = Instant.parse("2020-07-22T17:00:00Z"),
    duration = Duration.ofHours(2),
    volume = None,
    comment = Some("first task")
  )
  lazy val t2 = TaskEntity(
    taskId = t2Id,
    projectDbId = project1Id,
    deletedAt = Some(Instant.parse("2020-07-22T17:10:00Z")),
    owner = owner1Id,
    startedAt = Instant.parse("2020-07-22T17:00:00Z"),
    duration = Duration.ofHours(2),
    volume = None,
    comment = Some("deleted task")
  )
  lazy val t3 = TaskEntity(
    taskId = t3Id,
    projectDbId = project2Id,
    deletedAt = None,
    owner = owner1Id,
    startedAt = Instant.parse("2020-07-23T17:00:00Z"),
    duration = Duration.ofHours(2),
    volume = Some(8),
    comment = Some("high volume different project")
  )

  describe("TaskRepositorySpec read() should") {
    it("return list of tasks for two projects") {
      val result = zio.Runtime.default.unsafeRun(taskRepo.read(List(project1Id, project2Id)))
      result shouldMatchTo(List(t1, t2, t3))
    }
    it("return list of tasks for one project") {
      val result = zio.Runtime.default.unsafeRun(taskRepo.read(List(project2Id)))
      result shouldMatchTo(List(t3))
    }
    it("return empty list when tasks for given projects does not exist") {
      val result = zio.Runtime.default.unsafeRun(taskRepo.read(List(666)))
      result should be(List())
    }
  }

}
