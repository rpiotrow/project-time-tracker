package io.github.rpiotrow.ptt.write.repository

import java.time.{Duration, Instant}

import io.getquill.context.jdbc.{Decoders, JdbcContextBase}

trait QuillQuotes extends Decoders {
  this: JdbcContextBase[_, _] =>

  implicit class DurationQuotes(left: Duration) {
    def +(right: Duration) = quote(infix"$left + $right".as[Duration])
    def -(right: Duration) = quote(infix"$left - $right".as[Duration])
  }

  implicit class OverlapsQuotes(left: (Instant, Duration)) {
    def overlaps(right: (Instant, Duration)) =
      quote(
        infix"(${left._1}, interval '1 second' * ${left._2}) overlaps (${right._1}, interval '1 second' * ${right._2})"
          .as[Boolean]
      )
  }

}
