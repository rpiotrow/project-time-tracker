package io.github.rpiotrow.ptt.read.repository

import java.time.{Duration, LocalDateTime}

import io.getquill.context.jdbc.{Decoders, JdbcContextBase}
import io.getquill.context.sql.SqlContext
import org.postgresql.util.PGInterval

private trait LocalDateTimeQuotes {
  this: SqlContext[_, _] =>

  implicit class LocalDateTimeQuotes(left: LocalDateTime) {
    def >(right: LocalDateTime)  = quote(infix"$left > $right".as[Boolean])
    def <(right: LocalDateTime)  = quote(infix"$left < $right".as[Boolean])
    def >=(right: LocalDateTime) = quote(infix"$left >= $right".as[Boolean])
    def <=(right: LocalDateTime) = quote(infix"$left <= $right".as[Boolean])
  }
}
