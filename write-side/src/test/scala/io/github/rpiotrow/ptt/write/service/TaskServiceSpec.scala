package io.github.rpiotrow.ptt.write.service

import cats.implicits._
import cats.data.EitherT
import com.softwaremill.diffx.scalatest.DiffMatcher.matchTo
import io.github.rpiotrow.ptt.api.output.TaskOutput
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, TaskEntity}
import io.github.rpiotrow.ptt.write.repository.{DBResult, ProjectRepository, TaskRepository}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class TaskServiceSpec extends AnyFunSpec with ServiceSpecBase with MockFactory with should.Matchers {

  describe("add should") {
    it("create new task") {
      val projectRepository = mock[ProjectRepository]
      val taskRepository    = mock[TaskRepository]
      val readSideService   = mock[ReadSideService]
      val service           = new TaskServiceLive(taskRepository, projectRepository, readSideService, tnx)

      (projectRepository.get _).expects(projectId.value).returning(Option(project).pure[DBResult])
      (taskRepository.overlapping _)
        .expects(userId, taskInput.startedAt, taskInput.duration)
        .returning(List[TaskEntity]().pure[DBResult])
      (taskRepository.add _).expects(project.dbId, taskInput, userId).returning(task.pure[DBResult])
      (readSideService.taskAdded _)
        .expects(task)
        .returning(taskReadModel.pure[DBResult])

      val result = service.add(projectId, taskInput, userId).value.unsafeRunSync()

      result should matchTo(taskOutput.asRight[AppFailure])
    }
    it("do not create task when it overlaps another one") {
      val projectRepository = mock[ProjectRepository]
      val taskRepository    = mock[TaskRepository]
      val readSideService   = mock[ReadSideService]
      val service           = new TaskServiceLive(taskRepository, projectRepository, readSideService, tnx)

      (projectRepository.get _).expects(projectId.value).returning(Option(project).pure[DBResult])
      (taskRepository.overlapping _)
        .expects(userId, taskInput.startedAt, taskInput.duration)
        .returning(List[TaskEntity](task.copy(dbId = 22)).pure[DBResult])

      val result = service.add(projectId, taskInput, userId).value.unsafeRunSync()

      result should be(InvalidTimeSpan.asLeft[TaskOutput])
    }
    it("do not create task for non-existing project") {
      val projectRepository = mock[ProjectRepository]
      val taskRepository    = mock[TaskRepository]
      val readSideService   = mock[ReadSideService]
      val service           = new TaskServiceLive(taskRepository, projectRepository, readSideService, tnx)

      (projectRepository.get _).expects(projectId.value).returning(Option.empty[ProjectEntity].pure[DBResult])

      val result = service.add(projectId, taskInput, ownerId).value.unsafeRunSync()

      result should be(EntityNotFound("project with given projectId does not exists").asLeft[TaskOutput])
    }
  }

}
