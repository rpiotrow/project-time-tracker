package io.github.rpiotrow.ptt.read.service

import java.time.{Duration, Instant, OffsetDateTime}
import java.util.UUID

import eu.timepit.refined.auto._
import cats.implicits._
import com.softwaremill.diffx.scalatest.DiffMatcher._
import eu.timepit.refined.auto._
import io.github.rpiotrow.ptt.api.model.{ProjectId, TaskId, UserId}
import io.github.rpiotrow.ptt.api.output._
import io.github.rpiotrow.ptt.api.param._
import io.github.rpiotrow.ptt.read.entity.{ProjectEntity, TaskEntity}
import io.github.rpiotrow.ptt.read.repository.{EntityNotFound, ProjectRepository, RepositoryFailure, TaskRepository}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should
import zio.ZIO

class ProjectServiceSpec extends AnyFunSpec with MockFactory with should.Matchers {

  val owner1Id = UserId(UUID.randomUUID())
  val owner2Id = UserId(UUID.randomUUID())

  val p1 = ProjectEntity(
    dbId = 1,
    projectId = "project one",
    createdAt = Instant.parse("2020-07-29T15:00:00Z"),
    lastAddDurationAt = Instant.parse("2020-07-29T22:00:00Z"),
    deletedAt = None,
    owner = owner1Id,
    durationSum = Duration.ofHours(3)
  )
  val p2 = ProjectEntity(
    dbId = 2,
    projectId = "project two",
    createdAt = Instant.parse("2020-07-30T15:00:00Z"),
    lastAddDurationAt = Instant.parse("2020-07-30T22:00:00Z"),
    deletedAt = None,
    owner = owner2Id,
    durationSum = Duration.ofHours(4)
  )
  val t1 = TaskEntity(
    taskId = TaskId.random(),
    projectDbId = p1.dbId,
    deletedAt = None,
    owner = owner1Id,
    startedAt = Instant.parse("2020-07-29T17:00:00Z"),
    duration = Duration.ofHours(2),
    volume = None,
    comment = Some("first task")
  )
  val t2 = TaskEntity(
    taskId = TaskId.random(),
    projectDbId = p1.dbId,
    deletedAt = None,
    owner = owner1Id,
    startedAt = Instant.parse("2020-07-29T18:00:00Z"),
    duration = Duration.ofHours(1),
    volume = Some(4),
    comment = Some("second task")
  )
  val t3 = TaskEntity(
    taskId = TaskId.random(),
    projectDbId = p2.dbId,
    deletedAt = None,
    owner = owner2Id,
    startedAt = Instant.parse("2020-07-30T18:00:00Z"),
    duration = Duration.ofHours(4),
    volume = Some(2),
    comment = None
  )

  describe("ProjectService") {
    describe("one() should") {
      it("return ProjectOutput when project exists and has tasks") {
        val projectRepository = mock[ProjectRepository.Service]
        val taskRepository    = mock[TaskRepository.Service]

        val expectedOutput = ProjectOutput(
          projectId = "project one",
          createdAt = OffsetDateTime.parse("2020-07-29T15:00:00Z"),
          deletedAt = None,
          owner = owner1Id,
          durationSum = Duration.ofHours(3),
          tasks = List(
            TaskOutput(
              taskId = t1.taskId,
              owner = owner1Id,
              startedAt = OffsetDateTime.parse("2020-07-29T17:00:00Z"),
              duration = Duration.ofHours(2),
              volume = None,
              comment = Some("first task"),
              deletedAt = None
            ),
            TaskOutput(
              taskId = t2.taskId,
              owner = owner1Id,
              startedAt = OffsetDateTime.parse("2020-07-29T18:00:00Z"),
              duration = Duration.ofHours(1),
              volume = Some(4),
              comment = Some("second task"),
              deletedAt = None
            )
          )
        )

        val projectId: ProjectId = "project one"
        (projectRepository.one _).expects(projectId).returning(ZIO.succeed(p1))
        (taskRepository.read _).expects(List(p1.dbId)).returning(ZIO.succeed(List(t1, t2)))

        val service = ProjectService.live(projectRepository, taskRepository)
        val result  = zio.Runtime.default.unsafeRun(service.one("project one").either)

        result should matchTo(expectedOutput.asRight[RepositoryFailure])
      }
      it("return ProjectOutput when project exists and has no tasks") {
        val projectRepository = mock[ProjectRepository.Service]
        val taskRepository    = mock[TaskRepository.Service]

        val expectedOutput = ProjectOutput(
          projectId = "project one",
          createdAt = OffsetDateTime.parse("2020-07-29T15:00:00Z"),
          deletedAt = None,
          owner = owner1Id,
          durationSum = Duration.ofHours(3),
          tasks = List()
        )

        val projectId: ProjectId = "project one"
        (projectRepository.one _).expects(projectId).returning(ZIO.succeed(p1))
        (taskRepository.read _).expects(List(p1.dbId)).returning(ZIO.succeed(List()))

        val service = ProjectService.live(projectRepository, taskRepository)
        val result  = zio.Runtime.default.unsafeRun(service.one("project one").either)

        result should matchTo(expectedOutput.asRight[RepositoryFailure])
      }
      it("return error when project does not exist") {
        val projectRepository = mock[ProjectRepository.Service]
        val taskRepository    = mock[TaskRepository.Service]

        val projectId: ProjectId = "test"
        (projectRepository.one _).expects(projectId).returning(zio.IO.fail(EntityNotFound("test")))

        val service = ProjectService.live(projectRepository, taskRepository)
        val result  = zio.Runtime.default.unsafeRun(service.one("test").either)

        result should be(Left(EntityNotFound("test")))
      }
    }
    describe("list() should") {
      val params = ProjectListParams(List(), None, None, None, None, None, 0, 25)

      it("return empty list when project repository returns empty list") {
        val projectRepository = mock[ProjectRepository.Service]
        val taskRepository    = mock[TaskRepository.Service]

        (projectRepository.list _).expects(params).returning(zio.IO.succeed(List()))
        (taskRepository.read _).expects(List()).returning(zio.IO.succeed(List()))

        val service = ProjectService.live(projectRepository, taskRepository)
        val result  = zio.Runtime.default.unsafeRun(service.list(params))

        result should be(List())
      }
      it("return list of ProjectOutput with tasks") {
        val projectRepository = mock[ProjectRepository.Service]
        val taskRepository    = mock[TaskRepository.Service]

        val expectedOutput1 = ProjectOutput(
          projectId = "project one",
          createdAt = OffsetDateTime.parse("2020-07-29T15:00:00Z"),
          deletedAt = None,
          owner = owner1Id,
          durationSum = Duration.ofHours(3),
          tasks = List(
            TaskOutput(
              taskId = t1.taskId,
              owner = owner1Id,
              startedAt = OffsetDateTime.parse("2020-07-29T17:00:00Z"),
              duration = Duration.ofHours(2),
              volume = None,
              comment = Some("first task"),
              deletedAt = None
            ),
            TaskOutput(
              taskId = t2.taskId,
              owner = owner1Id,
              startedAt = OffsetDateTime.parse("2020-07-29T18:00:00Z"),
              duration = Duration.ofHours(1),
              volume = Some(4),
              comment = Some("second task"),
              deletedAt = None
            )
          )
        )
        val expectedOutput2 = ProjectOutput(
          projectId = "project two",
          createdAt = OffsetDateTime.parse("2020-07-30T15:00:00Z"),
          deletedAt = None,
          owner = owner2Id,
          durationSum = Duration.ofHours(4),
          tasks = List(
            TaskOutput(
              taskId = t3.taskId,
              owner = owner2Id,
              startedAt = OffsetDateTime.parse("2020-07-30T18:00:00Z"),
              duration = Duration.ofHours(4),
              volume = Some(2),
              comment = None,
              deletedAt = None
            )
          )
        )

        (projectRepository.list _).expects(params).returning(zio.IO.succeed(List(p1, p2)))
        (taskRepository.read _).expects(List(p1.dbId, p2.dbId)).returning(zio.IO.succeed(List(t1, t2, t3)))

        val service = ProjectService.live(projectRepository, taskRepository)
        val result  = zio.Runtime.default.unsafeRun(service.list(params))

        result should matchTo(List(expectedOutput1, expectedOutput2))
      }
      it("return list of ProjectOutput when some projects does not have tasks") {
        val projectRepository = mock[ProjectRepository.Service]
        val taskRepository    = mock[TaskRepository.Service]

        val expectedOutput1 = ProjectOutput(
          projectId = "project one",
          createdAt = OffsetDateTime.parse("2020-07-29T15:00:00Z"),
          deletedAt = None,
          owner = owner1Id,
          durationSum = Duration.ofHours(3),
          tasks = List()
        )
        val expectedOutput2 = ProjectOutput(
          projectId = "project two",
          createdAt = OffsetDateTime.parse("2020-07-30T15:00:00Z"),
          deletedAt = None,
          owner = owner2Id,
          durationSum = Duration.ofHours(4),
          tasks = List(
            TaskOutput(
              taskId = t3.taskId,
              owner = owner2Id,
              startedAt = OffsetDateTime.parse("2020-07-30T18:00:00Z"),
              duration = Duration.ofHours(4),
              volume = Some(2),
              comment = None,
              deletedAt = None
            )
          )
        )

        val params = ProjectListParams(List(), None, None, None, None, None, 0, 25)
        (projectRepository.list _).expects(params).returning(zio.IO.succeed(List(p1, p2)))
        (taskRepository.read _).expects(List(p1.dbId, p2.dbId)).returning(zio.IO.succeed(List(t3)))

        val service = ProjectService.live(projectRepository, taskRepository)
        val result  = zio.Runtime.default.unsafeRun(service.list(params))

        result should matchTo(List(expectedOutput1, expectedOutput2))
      }
      it("return list of ProjectOutput when all projects does not have tasks") {
        val projectRepository = mock[ProjectRepository.Service]
        val taskRepository    = mock[TaskRepository.Service]

        val expectedOutput1 = ProjectOutput(
          projectId = "project one",
          createdAt = OffsetDateTime.parse("2020-07-29T15:00:00Z"),
          deletedAt = None,
          owner = owner1Id,
          durationSum = Duration.ofHours(3),
          tasks = List()
        )
        val expectedOutput2 = ProjectOutput(
          projectId = "project two",
          createdAt = OffsetDateTime.parse("2020-07-30T15:00:00Z"),
          deletedAt = None,
          owner = owner2Id,
          durationSum = Duration.ofHours(4),
          tasks = List()
        )

        (projectRepository.list _).expects(params).returning(zio.IO.succeed(List(p1, p2)))
        (taskRepository.read _).expects(List(p1.dbId, p2.dbId)).returning(zio.IO.succeed(List()))

        val service = ProjectService.live(projectRepository, taskRepository)
        val result  = zio.Runtime.default.unsafeRun(service.list(params))

        result should matchTo(List(expectedOutput1, expectedOutput2))
      }
    }
  }

}
