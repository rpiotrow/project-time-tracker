package io.github.rpiotrow.ptt.read.repository

import java.time.Duration

import io.getquill.context.jdbc.{Decoders, JdbcContextBase}
import org.postgresql.util.PGInterval

private trait DurationDecoder extends Decoders {
  this: JdbcContextBase[_, _] =>

  implicit val decodeDuration = MappedEncoding[Long, Duration](Duration.ofMinutes(_))
}
