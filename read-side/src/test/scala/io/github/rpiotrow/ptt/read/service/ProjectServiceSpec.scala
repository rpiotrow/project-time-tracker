package io.github.rpiotrow.ptt.read.service

import java.time.{Duration, LocalDateTime}
import java.util.UUID

import cats.implicits._
import com.softwaremill.diffx.scalatest.DiffMatcher._
import eu.timepit.refined.auto._
import io.github.rpiotrow.projecttimetracker.api.output.{ProjectOutput, TaskOutput}
import io.github.rpiotrow.ptt.read.entity.{ProjectEntity, TaskEntity}
import io.github.rpiotrow.ptt.read.repository.ProjectRepository.ProjectListSearchParams
import io.github.rpiotrow.ptt.read.repository.{EntityNotFound, ProjectRepository, RepositoryFailure, TaskRepository}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should
import zio.ZIO

class ProjectServiceSpec extends AnyFunSpec with MockFactory with should.Matchers {

  val owner1Id = UUID.randomUUID()
  val owner2Id = UUID.randomUUID()

  val p1 = ProjectEntity(
    dbId = 1,
    id = "project one",
    createdAt = LocalDateTime.of(2020, 7, 29, 15, 0),
    updatedAt = LocalDateTime.of(2020, 7, 29, 22, 0),
    deletedAt = None,
    owner = owner1Id,
    durationSum = Duration.ofHours(3)
  )
  val p2 = ProjectEntity(
    dbId = 2,
    id = "project two",
    createdAt = LocalDateTime.of(2020, 7, 30, 15, 0),
    updatedAt = LocalDateTime.of(2020, 7, 30, 22, 0),
    deletedAt = None,
    owner = owner2Id,
    durationSum = Duration.ofHours(4)
  )
  val t1 = TaskEntity(
    projectId = p1.dbId,
    deletedAt = None,
    owner = owner1Id,
    startedAt = LocalDateTime.of(2020, 7, 29, 17, 0),
    duration = Duration.ofHours(2),
    volume = None,
    comment = Some("first task")
  )
  val t2 = TaskEntity(
    projectId = p1.dbId,
    deletedAt = None,
    owner = owner1Id,
    startedAt = LocalDateTime.of(2020, 7, 29, 18, 0),
    duration = Duration.ofHours(1),
    volume = Some(4),
    comment = Some("second task")
  )
  val t3 = TaskEntity(
    projectId = p2.dbId,
    deletedAt = None,
    owner = owner2Id,
    startedAt = LocalDateTime.of(2020, 7, 30, 18, 0),
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
          id = "project one",
          createdAt = LocalDateTime.of(2020, 7, 29, 15, 0),
          owner = owner1Id,
          durationSum = Duration.ofHours(3),
          tasks = List(
            TaskOutput(
              owner = owner1Id,
              startedAt = LocalDateTime.of(2020, 7, 29, 17, 0),
              duration = Duration.ofHours(2),
              volume = None,
              comment = Some("first task")
            ),
            TaskOutput(
              owner = owner1Id,
              startedAt = LocalDateTime.of(2020, 7, 29, 18, 0),
              duration = Duration.ofHours(1),
              volume = Some(4),
              comment = Some("second task")
            )
          )
        )

        (projectRepository.one _).expects("project one").returning(ZIO.succeed(p1))
        (taskRepository.read _).expects(List(p1.dbId)).returning(ZIO.succeed(List(t1, t2)))

        val service = ProjectService.live(projectRepository, taskRepository)
        val result  = zio.Runtime.default.unsafeRun(service.one("project one").either)

        result should matchTo(expectedOutput.asRight[RepositoryFailure])
      }
      it("return ProjectOutput when project exists and has no tasks") {
        val projectRepository = mock[ProjectRepository.Service]
        val taskRepository    = mock[TaskRepository.Service]

        val expectedOutput = ProjectOutput(
          id = "project one",
          createdAt = LocalDateTime.of(2020, 7, 29, 15, 0),
          owner = owner1Id,
          durationSum = Duration.ofHours(3),
          tasks = List()
        )

        (projectRepository.one _).expects("project one").returning(ZIO.succeed(p1))
        (taskRepository.read _).expects(List(p1.dbId)).returning(ZIO.succeed(List()))

        val service = ProjectService.live(projectRepository, taskRepository)
        val result  = zio.Runtime.default.unsafeRun(service.one("project one").either)

        result should matchTo(expectedOutput.asRight[RepositoryFailure])
      }
      it("return error when project does not exist") {
        val projectRepository = mock[ProjectRepository.Service]
        val taskRepository    = mock[TaskRepository.Service]

        (projectRepository.one _).expects("test").returning(zio.IO.fail(EntityNotFound))

        val service = ProjectService.live(projectRepository, taskRepository)
        val result  = zio.Runtime.default.unsafeRun(service.one("test").either)

        result should be(Left(EntityNotFound))
      }
    }
    describe("list() should") {
      it("return empty list when project repository returns empty list") {
        val projectRepository = mock[ProjectRepository.Service]
        val taskRepository    = mock[TaskRepository.Service]

        val params = ProjectListSearchParams(List(), None, None, None, None, None, 0, 25)
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
          id = "project one",
          createdAt = LocalDateTime.of(2020, 7, 29, 15, 0),
          owner = owner1Id,
          durationSum = Duration.ofHours(3),
          tasks = List(
            TaskOutput(
              owner = owner1Id,
              startedAt = LocalDateTime.of(2020, 7, 29, 17, 0),
              duration = Duration.ofHours(2),
              volume = None,
              comment = Some("first task")
            ),
            TaskOutput(
              owner = owner1Id,
              startedAt = LocalDateTime.of(2020, 7, 29, 18, 0),
              duration = Duration.ofHours(1),
              volume = Some(4),
              comment = Some("second task")
            )
          )
        )
        val expectedOutput2 = ProjectOutput(
          id = "project two",
          createdAt = LocalDateTime.of(2020, 7, 30, 15, 0),
          owner = owner2Id,
          durationSum = Duration.ofHours(4),
          tasks = List(
            TaskOutput(
              owner = owner2Id,
              startedAt = LocalDateTime.of(2020, 7, 30, 18, 0),
              duration = Duration.ofHours(4),
              volume = Some(2),
              comment = None
            )
          )
        )

        val params = ProjectListSearchParams(List(), None, None, None, None, None, 0, 25)
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
          id = "project one",
          createdAt = LocalDateTime.of(2020, 7, 29, 15, 0),
          owner = owner1Id,
          durationSum = Duration.ofHours(3),
          tasks = List()
        )
        val expectedOutput2 = ProjectOutput(
          id = "project two",
          createdAt = LocalDateTime.of(2020, 7, 30, 15, 0),
          owner = owner2Id,
          durationSum = Duration.ofHours(4),
          tasks = List(
            TaskOutput(
              owner = owner2Id,
              startedAt = LocalDateTime.of(2020, 7, 30, 18, 0),
              duration = Duration.ofHours(4),
              volume = Some(2),
              comment = None
            )
          )
        )

        val params = ProjectListSearchParams(List(), None, None, None, None, None, 0, 25)
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
          id = "project one",
          createdAt = LocalDateTime.of(2020, 7, 29, 15, 0),
          owner = owner1Id,
          durationSum = Duration.ofHours(3),
          tasks = List()
        )
        val expectedOutput2 = ProjectOutput(
          id = "project two",
          createdAt = LocalDateTime.of(2020, 7, 30, 15, 0),
          owner = owner2Id,
          durationSum = Duration.ofHours(4),
          tasks = List()
        )

        val params = ProjectListSearchParams(List(), None, None, None, None, None, 0, 25)
        (projectRepository.list _).expects(params).returning(zio.IO.succeed(List(p1, p2)))
        (taskRepository.read _).expects(List(p1.dbId, p2.dbId)).returning(zio.IO.succeed(List()))

        val service = ProjectService.live(projectRepository, taskRepository)
        val result  = zio.Runtime.default.unsafeRun(service.list(params))

        result should matchTo(List(expectedOutput1, expectedOutput2))
      }
    }
  }

}
