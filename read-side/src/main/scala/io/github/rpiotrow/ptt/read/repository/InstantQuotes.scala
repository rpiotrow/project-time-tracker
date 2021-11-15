package io.github.rpiotrow.ptt.read.repository

import java.time.Instant

import io.getquill.context.sql.SqlContext

private trait InstantQuotes {
  this: SqlContext[_, _] =>

  implicit class InstantQuotes(left: Instant) {
    def >(right: Instant)  = quote(infix"$left > $right".as[Boolean])
    def <(right: Instant)  = quote(infix"$left < $right".as[Boolean])
    def >=(right: Instant) = quote(infix"$left >= $right".as[Boolean])
    def <=(right: Instant) = quote(infix"$left <= $right".as[Boolean])
  }
}
