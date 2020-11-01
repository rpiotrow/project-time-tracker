package io.github.rpiotrow.ptt.read.repository

import java.time.{Duration, Instant, LocalDateTime, ZoneOffset}

import eu.timepit.refined.api.Refined
import io.getquill.context.jdbc.{Decoders, JdbcContextBase}
import io.github.rpiotrow.ptt.api.model.ProjectId

private trait CustomDecoders extends Decoders {
  this: JdbcContextBase[_, _] =>

  implicit val decodeDuration = MappedEncoding[Long, Duration](Duration.ofSeconds)

  implicit val decodeProjectId = MappedEncoding[String, ProjectId](Refined.unsafeApply)

  implicit val decodeInstant = MappedEncoding[LocalDateTime, Instant](_.toInstant(ZoneOffset.UTC))
  implicit val encodeInstant = MappedEncoding[Instant, LocalDateTime](LocalDateTime.ofInstant(_, ZoneOffset.UTC))
}
