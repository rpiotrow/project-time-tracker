package io.github.rpiotrow.ptt.read.repository

import doobie.Transactor
import doobie.implicits._
import doobie.quill.DoobieContext
import io.getquill.{SnakeCase, idiom => _}
import io.github.rpiotrow.ptt.read.entity.{ProjectEntity, TaskEntity}
import zio.{IO, Task}
import zio.interop.catz._

object TaskRepository {
  trait Service {
    def read(projectIds: List[Long]): IO[RepositoryThrowable, List[TaskEntity]]
  }
  def live(tnx: Transactor[Task]): Service =
    new Service {
      private val dc = new DoobieContext.Postgres(SnakeCase) with DurationDecoder
      import dc._

      private val tasks = quote { querySchema[TaskEntity]("tasks") }

      override def read(projectIds: List[Long]): IO[RepositoryThrowable, List[TaskEntity]] = {
        run(quote {
          tasks.filter(e => liftQuery(projectIds).contains(e.projectDbId))
        })
          .transact(tnx)
          .mapError(RepositoryThrowable)
      }
    }
}
