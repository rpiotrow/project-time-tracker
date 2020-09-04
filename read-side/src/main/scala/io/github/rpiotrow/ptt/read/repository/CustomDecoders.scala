package io.github.rpiotrow.ptt.read.repository

import java.time.Duration

import eu.timepit.refined.api.Refined
import io.getquill.context.jdbc.{Decoders, JdbcContextBase}
import io.github.rpiotrow.ptt.api.model.ProjectId
import org.postgresql.util.PGInterval

private trait CustomDecoders extends Decoders {
  this: JdbcContextBase[_, _] =>

  implicit val decodeDuration = MappedEncoding[Long, Duration](Duration.ofSeconds)

  implicit val decodeProjectId = MappedEncoding[String, ProjectId](Refined.unsafeApply)
}
