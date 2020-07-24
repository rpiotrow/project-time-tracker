package io.github.rpiotrow.ptt.read.repository

import java.time.Duration

import io.getquill.context.jdbc.{Decoders, JdbcContextBase}
import org.postgresql.util.PGInterval

private trait DurationDecoder extends Decoders {
  this: JdbcContextBase[_, _] =>

  implicit val durationDecoder: Decoder[Duration] =
    decoder((index, row) => {
      val pgInterval = row.getObject(index, classOf[PGInterval])
      val seconds    = pgInterval.getHours() * 60 * 60 + pgInterval.getMinutes() * 60 + pgInterval.getWholeSeconds()
      Duration.ofSeconds(seconds)
    })
}
