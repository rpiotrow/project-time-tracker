package io.github.rpiotrow.ptt.write.repository

import java.time.{Duration, LocalDateTime}

import io.getquill.context.jdbc.{Decoders, JdbcContextBase}

trait DurationMapping extends Decoders {
  this: JdbcContextBase[_, _] =>

  implicit val decodeDuration = MappedEncoding[Long, Duration](Duration.ofSeconds)
  implicit val encodeDuration = MappedEncoding[Duration, Long](_.toSeconds)

  implicit class DurationQuotes(left: Duration) {
    def +(right: Duration) = quote(infix"$left + $right".as[Duration])
  }

  implicit class OverlapsQuotes(left: (LocalDateTime, Duration)) {
    def overlaps(right: (LocalDateTime, Duration)) =
      quote(
        infix"(${left._1}, interval '1 second' * ${left._2}) overlaps (${right._1}, interval '1 second' * ${right._2})"
          .as[Boolean]
      )
  }
}
