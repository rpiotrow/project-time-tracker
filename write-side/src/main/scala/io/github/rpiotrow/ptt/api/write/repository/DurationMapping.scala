package io.github.rpiotrow.ptt.api.write.repository

import java.time.Duration

import io.getquill.context.jdbc.{Decoders, JdbcContextBase}

trait DurationMapping extends Decoders {
  this: JdbcContextBase[_, _] =>

  implicit val decodeDuration = MappedEncoding[Long, Duration](Duration.ofSeconds(_))
  implicit val encodeDuration = MappedEncoding[Duration, Long](_.getSeconds)
}
