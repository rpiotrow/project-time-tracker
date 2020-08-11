package io.github.rpiotrow.ptt.api.write

import doobie.quill.DoobieContext
import io.getquill.SnakeCase
import zio._

package object repository {
  type DBContext = DoobieContext.Postgres[SnakeCase.type] with DurationMapping

  type ProjectRepository         = Has[ProjectRepository.Service]
  type ProjectReadSideRepository = Has[ProjectReadSideRepository.Service]

  type Repositories = ProjectRepository with ProjectReadSideRepository

  val live: ZLayer[Any, Throwable, Repositories] = ZLayer.fromFunctionMany { _ =>
    val ctx = new DoobieContext.Postgres(SnakeCase) with DurationMapping
    Has(ProjectRepository.live(ctx)) ++ Has(ProjectReadSideRepository.live(ctx))
  }
}
