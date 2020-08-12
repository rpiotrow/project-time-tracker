package io.github.rpiotrow.ptt.write

import doobie.ConnectionIO
import doobie.quill.DoobieContext
import io.getquill.SnakeCase

package object repository {
  type DBContext   = DoobieContext.Postgres[SnakeCase.type] with DurationMapping
  type DBResult[A] = ConnectionIO[A]

  lazy val liveContext: DBContext = new DoobieContext.Postgres(SnakeCase) with DurationMapping
}
