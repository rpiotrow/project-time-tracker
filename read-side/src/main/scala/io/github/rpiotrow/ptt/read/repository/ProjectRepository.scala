package io.github.rpiotrow.ptt.read.repository

import java.time.LocalDateTime

import doobie.Transactor
import doobie.implicits._
import doobie.quill.DoobieContext
import io.getquill.{idiom => _, _}
import io.github.rpiotrow.projecttimetracker.api.param.OrderingDirection._
import io.github.rpiotrow.projecttimetracker.api.param.ProjectOrderingField._
import io.github.rpiotrow.projecttimetracker.api.param._
import io.github.rpiotrow.ptt.read.entity.ProjectEntity
import io.github.rpiotrow.ptt.read.repository.ProjectRepository.ProjectListSearchParams
import zio.{IO, Task, ZIO}
import zio.interop.catz._

object ProjectRepository {
  case class ProjectListSearchParams(
    ids: List[String],
    from: Option[LocalDateTime],
    to: Option[LocalDateTime],
    deleted: Option[Boolean],
    orderBy: Option[ProjectOrderingField],
    orderDirection: Option[OrderingDirection],
    pageNumber: PageNumber,
    pageSize: PageSize
  )

  trait Service {
    def one(id: String): IO[RepositoryFailure, ProjectEntity]
    def list(params: ProjectListSearchParams): IO[RepositoryThrowable, List[ProjectEntity]]
  }

  def live(tnx: Transactor[Task]): Service = new ProjectRepositoryLive(tnx)
}

private class ProjectRepositoryLive(private val tnx: Transactor[Task]) extends ProjectRepository.Service {

  private val dc = new DoobieContext.Postgres(SnakeCase) with LocalDateTimeQuotes with DurationDecoder
  import dc._

  private val projects = quote { querySchema[ProjectEntity]("projects") }

  override def one(id: String) = {
    run(projects.filter(_.id == lift(id)))
      .map(_.headOption)
      .transact(tnx)
      .mapError(RepositoryThrowable)
      .flatMap(ZIO.fromOption(_).orElseFail(EntityNotFound))
  }

  override def list(params: ProjectListSearchParams) = {
    run(
      projects.dynamic
        .filterIf(params.ids.nonEmpty)(p => quote(liftQuery(params.ids).contains(p.id)))
        .filterIf(params.deleted.contains(true))(p => quote(p.deletedAt.isDefined))
        .filterIf(params.deleted.contains(false))(p => quote(p.deletedAt.isEmpty))
        .filterOpt(params.from)((p, from) => quote(p.createdAt >= from))
        .filterOpt(params.to)((p, to) => quote(p.createdAt <= to))
        .sortBy(p => projectOrderingField(params.orderBy, p))(orderingDirection(params.orderDirection))
        .drop(params.pageNumber.value * params.pageSize.value)
        .take(params.pageSize.value)
    )
      .transact(tnx)
      .mapError(RepositoryThrowable)
  }

  private def projectOrderingField[T](order: Option[ProjectOrderingField], p: dc.Quoted[ProjectEntity]) = {
    order match {
      case Some(CreatedAt) => quote(p.createdAt)
      case Some(UpdatedAt) => quote(p.updatedAt)
      case None            => quote(p.dbId)
    }
  }

  private def orderingDirection[T](direction: Option[OrderingDirection]): Ord[T] = {
    direction match {
      case Some(Descending) => Ord.desc
      case _                => Ord.asc
    }
  }
}
