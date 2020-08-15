package io.github.rpiotrow.ptt.write.service

import cats.implicits._
import doobie.implicits._
import io.github.rpiotrow.ptt.write.repository.{DBResult, ProjectReadSideRepository}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class ReadSideServiceSpec extends AnyFunSpec with ServiceSpecBase with MockFactory with should.Matchers {

  describe("projectCreated should") {
    it("create project in read model") {
      val projectReadSideRepository = mock[ProjectReadSideRepository]
      val service                   = new ReadSideServiceLive(projectReadSideRepository)

      (projectReadSideRepository.newProject _).expects(project).returning(projectReadModel.pure[DBResult])
      val result = service.projectCreated(project).transact(tnx).value.unsafeRunSync()

      result should be(projectReadModel.asRight[AppFailure])
    }
  }

  describe("projectDeleted should") {
    it("mark project as deleted in read model") {
      val projectReadSideRepository = mock[ProjectReadSideRepository]
      val service                   = new ReadSideServiceLive(projectReadSideRepository)

      (projectReadSideRepository.deleteProject _).expects(project).returning(projectReadModel.pure[DBResult])
      val result = service.projectDeleted(project).transact(tnx).value.unsafeRunSync()

      result should be(projectReadModel.asRight[AppFailure])
    }
  }

}
