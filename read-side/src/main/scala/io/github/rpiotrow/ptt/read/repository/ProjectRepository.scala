package io.github.rpiotrow.ptt.read.repository

import eu.timepit.refined.auto._
import io.getquill.context.ZioJdbc._
import io.getquill.context.qzio.ImplicitSyntax.Implicit
import io.getquill.{idiom => _, _}
import io.github.rpiotrow.ptt.api.model.ProjectId
import io.github.rpiotrow.ptt.api.param.OrderingDirection._
import io.github.rpiotrow.ptt.api.param.ProjectOrderingField._
import io.github.rpiotrow.ptt.api.param._
import io.github.rpiotrow.ptt.read.entity.ProjectEntity
import zio.{Has, IO, ZIO}

object ProjectRepository {
  trait Service {
    def one(projectId: ProjectId): IO[RepositoryFailure, ProjectEntity]
    def list(params: ProjectListParams): IO[RepositoryThrowable, List[ProjectEntity]]
  }

  def live(ds: DataSourceCloseable): Service = new ProjectRepositoryLive(ds)
}

private class ProjectRepositoryLive(private val ds: DataSourceCloseable) extends ProjectRepository.Service {

  import quillContext._
  implicit private val implicitDS: Implicit[Has[DataSourceCloseable]] = Implicit(Has(ds))

  private val projects = quote { querySchema[ProjectEntity]("projects") }

  override def one(projectId: ProjectId): IO[RepositoryFailure, ProjectEntity] = {
    run(projects.filter(_.projectId == lift(projectId)))
      .map(_.headOption)
      .implicitDS
      .mapError(RepositoryThrowable)
      .flatMap(ZIO.fromOption(_).orElseFail(EntityNotFound(projectId.value)))
  }

  override def list(params: ProjectListParams): IO[RepositoryThrowable, List[ProjectEntity]] = {
    val projectIds = params.ids.map(_.value)
    val pageNumber = params.pageNumber
    val pageSize   = params.pageSize
    run(
      projects.dynamic
        .filterIf(params.ids.nonEmpty)(p => quote(liftQuery(projectIds).contains(p.projectId)))
        .filterIf(params.deleted.contains(true))(p => quote(p.deletedAt.isDefined))
        .filterIf(params.deleted.contains(false))(p => quote(p.deletedAt.isEmpty))
        .filterOpt(params.from.map(_.toInstant))((p, from) => quote(p.createdAt >= from))
        .filterOpt(params.to.map(_.toInstant))((p, to) => quote(p.createdAt <= to))
        .sortBy(p => projectOrderingField(params.orderBy, p))(orderingDirection(params.orderDirection))
        .drop(pageNumber * pageSize)
        .take(pageSize)
    )
      .implicitDS
      .mapError(RepositoryThrowable)
  }

  private def projectOrderingField[T](order: Option[ProjectOrderingField], p: Quoted[ProjectEntity]) = {
    order match {
      case Some(CreatedAt)         => quote(p.createdAt)
      case Some(LastAddDurationAt) => quote(p.lastAddDurationAt)
      case None                    => quote(p.dbId)
    }
  }

  private def orderingDirection[T](direction: Option[OrderingDirection]): Ord[T] = {
    direction match {
      case Some(Descending) => Ord.desc
      case _                => Ord.asc
    }
  }
}
