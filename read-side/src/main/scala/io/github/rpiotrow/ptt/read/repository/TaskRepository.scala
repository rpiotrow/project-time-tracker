package io.github.rpiotrow.ptt.read.repository

import io.getquill.context.ZioJdbc._
import io.getquill.context.qzio.ImplicitSyntax.Implicit
import io.github.rpiotrow.ptt.read.entity.TaskEntity
import zio.{Has, IO}

object TaskRepository {
  trait Service {
    def read(projectIds: List[Long]): IO[RepositoryThrowable, List[TaskEntity]]
  }
  def live(ds: DataSourceCloseable): Service =
    new Service {
      import quillContext._
      implicit private val implicitDS: Implicit[Has[DataSourceCloseable]] = Implicit(Has(ds))

      private val tasks = quote { querySchema[TaskEntity]("tasks") }

      override def read(projectIds: List[Long]): IO[RepositoryThrowable, List[TaskEntity]] = {
        run(quote {
          tasks.filter(e => liftQuery(projectIds).contains(e.projectDbId))
        })
          .implicitDS
          .mapError(RepositoryThrowable)
      }
    }
}
