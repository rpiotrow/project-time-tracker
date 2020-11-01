package io.github.rpiotrow.ptt.e2e

import java.time.{Clock, OffsetDateTime}

import eu.timepit.refined
import eu.timepit.refined.auto._
import io.github.rpiotrow.ptt.api.input._
import io.github.rpiotrow.ptt.api.model._
import io.github.rpiotrow.ptt.api.param.OrderingDirection._
import io.github.rpiotrow.ptt.api.param.ProjectOrderingField._
import io.github.rpiotrow.ptt.api.param._
import io.github.rpiotrow.ptt.e2e.factories.ProjectFactory._
import io.github.rpiotrow.ptt.e2e.factories.TaskFactory._
import io.github.rpiotrow.ptt.e2e.factories.UserIdFactory._
import io.github.rpiotrow.ptt.e2e.utils._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

import scala.util.Random

class ProjectListEnd2EndTests extends AnyFunSpec with should.Matchers with End2EndTestsBase with ApiResults {

  import ApiClient._

  describe("project paging and sorting") {
    it("created at") {
      val userId   = generateUserId()
      val projects = List.fill(5)(createProjectOnly)

      val pages     = List.range(0, 4)
      val ascending = gatherPagedResults(pages, createParams(projects, 2, CreatedAt, Ascending), userId)
      ascending should be(
        List(List(projects(0), projects(1)), List(projects(2), projects(3)), List(projects(4)), List())
      )

      val descending = gatherPagedResults(pages, createParams(projects, 2, CreatedAt, Descending), userId)
      descending should be(
        List(List(projects(4), projects(3)), List(projects(2), projects(1)), List(projects(0)), List())
      )
    }
    it("last add duration at") {
      val userId   = generateUserId()
      val projects = Random.shuffle(List.fill(7)(createProjectOnly))
      projects.foreach(p => createTask(p, generateTaskInput(), generateUserId()).success())

      val pages     = List.range(0, 4)
      val ascending = gatherPagedResults(pages, createParams(projects, 3, LastAddDurationAt, Ascending), userId)
      ascending should be(
        List(
          List(projects(0), projects(1), projects(2)),
          List(projects(3), projects(4), projects(5)),
          List(projects(6)),
          List()
        )
      )

      val descending = gatherPagedResults(pages, createParams(projects, 3, LastAddDurationAt, Descending), userId)
      descending should be(
        List(
          List(projects(6), projects(5), projects(4)),
          List(projects(3), projects(2), projects(1)),
          List(projects(0)),
          List()
        )
      )
    }
  }

  describe("project filtering") {
    it("deleted and not-deleted") {
      val projectsWithOwner = List.fill(5)(createProjectWithOwner)
      val projects          = projectsWithOwner.map(_._1)
      projectsWithOwner.zipWithIndex.foreach({
        case ((projectId, ownerId), index) => if (index % 2 == 1) deleteProject(projectId, ownerId)
      })

      val params = ProjectListParams(
        ids = projectsWithOwner.map(_._1),
        from = None,
        to = None,
        deleted = Some(true),
        orderBy = Some(CreatedAt),
        orderDirection = Some(Ascending),
        pageNumber = 0,
        pageSize = 25
      )

      val deleted = projectList(params, generateUserId()).success().map(_.projectId)
      deleted should be(List(projects(1), projects(3)))

      val notDeleted = projectList(params.copy(deleted = Some(false)), generateUserId()).success().map(_.projectId)
      notDeleted should be(List(projects(0), projects(2), projects(4)))
    }
    it("from-to") {
      val utcClock = Clock.systemUTC()
      val projects = List.fill(3)(createProjectOnly)

      val params = ProjectListParams(
        ids = projects,
        from = Some(OffsetDateTime.now(utcClock).minusHours(1)),
        to = Some(OffsetDateTime.now(utcClock).plusHours(1)),
        deleted = None,
        orderBy = Some(CreatedAt),
        orderDirection = Some(Ascending),
        pageNumber = 0,
        pageSize = 25
      )

      val fromTo = projectList(params, generateUserId()).success().map(_.projectId)
      fromTo should be(projects)

      val outOfTimeRange = projectList(
        params.copy(
          from = Some(OffsetDateTime.now(utcClock).plusHours(1)),
          to = Some(OffsetDateTime.now(utcClock).plusHours(2))
        ),
        generateUserId()
      ).success().map(_.projectId)
      outOfTimeRange should be(List())
    }
  }

  private def gatherPagedResults(pages: List[Int], params: ProjectListParams, userId: UserId) =
    pages.map(
      pageNumber =>
        projectList(params.copy(pageNumber = createPageNumber(pageNumber)), userId).success().map(_.projectId)
    )

  private def createPageNumber(value: Int) = {
    val either: Either[String, PageNumber] = refined.refineV(value)
    either.fold(err => throw new IllegalArgumentException(err), identity)
  }

  private def createParams(
    projects: List[ProjectId],
    pageSize: PageSize,
    orderingField: ProjectOrderingField,
    orderingDirection: OrderingDirection
  ) =
    ProjectListParams(
      ids = projects,
      from = None,
      to = None,
      deleted = None,
      orderBy = Some(orderingField),
      orderDirection = Some(orderingDirection),
      pageNumber = 0,
      pageSize = pageSize
    )

  private def createProjectOnly: ProjectId = {
    val projectId = generateProjectId()
    createProject(ProjectInput(projectId), generateUserId()).success()
    projectId
  }

}
