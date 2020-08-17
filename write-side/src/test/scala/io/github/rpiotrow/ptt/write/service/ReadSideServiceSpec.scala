package io.github.rpiotrow.ptt.write.service

import cats.implicits._
import doobie.implicits._
import io.github.rpiotrow.ptt.write.repository.{DBResult, ProjectReadSideRepository, TaskReadSideRepository}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class ReadSideServiceSpec extends AnyFunSpec with ServiceSpecBase with MockFactory with should.Matchers {

  describe("projectCreated should") {
    it("create project in read model") {
      val projectReadSideRepository = mock[ProjectReadSideRepository]
      val taskReadSideRepository    = mock[TaskReadSideRepository]
      val service                   = new ReadSideServiceLive(projectReadSideRepository, taskReadSideRepository)

      (projectReadSideRepository.newProject _).expects(project).returning(projectReadModel.pure[DBResult])
      val result = service.projectCreated(project).transact(tnx).value.unsafeRunSync()

      result should be(projectReadModel.asRight[AppFailure])
    }
  }

  describe("projectDeleted should") {
    it("update project, tasks and statistics in the read model") {
      val projectReadSideRepository = mock[ProjectReadSideRepository]
      val taskReadSideRepository    = mock[TaskReadSideRepository]
      val service                   = new ReadSideServiceLive(projectReadSideRepository, taskReadSideRepository)

      (projectReadSideRepository.deleteProject _).expects(project).returning(().pure[DBResult])
      // TODO: delete all tasks related to project on read side
      // TODO: update statistics
      val result = service.projectDeleted(project).transact(tnx).value.unsafeRunSync()

      result should be(().asRight[AppFailure])
    }
  }

  describe("newTask should") {
    it("update project, task and statistics in the read model") {
      val projectReadSideRepository = mock[ProjectReadSideRepository]
      val taskReadSideRepository    = mock[TaskReadSideRepository]
      val service                   = new ReadSideServiceLive(projectReadSideRepository, taskReadSideRepository)

      (taskReadSideRepository.add _).expects(task).returning(taskReadModel.pure[DBResult])
      (projectReadSideRepository.addToProjectDuration _).expects(taskReadModel).returning(().pure[DBResult])
      val result = service.taskAdded(task).transact(tnx).value.unsafeRunSync()

      result should be(taskReadModel.asRight[AppFailure])
    }
  }

}
