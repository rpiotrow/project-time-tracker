package io.github.rpiotrow.ptt.write.service

import java.time.{Duration, Instant, LocalDateTime, LocalTime, Period, YearMonth, ZoneOffset, OffsetDateTime}

case class InstantRange(from: Instant, to: Instant) {
  lazy val fromUtc                  = OffsetDateTime.ofInstant(from, ZoneOffset.UTC)
  lazy val toUtc                    = OffsetDateTime.ofInstant(to, ZoneOffset.UTC)
  lazy val fromYearMonth: YearMonth = YearMonth.of(fromUtc.getYear, fromUtc.getMonth)
  lazy val endYearMonth: YearMonth  = YearMonth.of(toUtc.getYear, toUtc.getMonth)

  def intersection(other: InstantRange): Option[InstantRange] = {
    val start = if (this.from.isAfter(other.from)) this.from else other.from
    val end   = if (this.to.isBefore(other.to)) this.to else other.to
    if (start.isBefore(end))
      Some(InstantRange(start, end))
    else
      None
  }

  def duration(): Duration = Duration.between(from, to)

  def splitToMonths(): List[InstantRange] = {
    if (fromYearMonth == endYearMonth) {
      List(this)
    } else {
      val months = Period.between(fromYearMonth.atDay(1), endYearMonth.atDay(1)).getMonths
      (0 to months).map(i => InstantRange.forYearMonth(fromYearMonth.plusMonths(i)).intersection(this).get).toList
    }
  }
}

object InstantRange {

  def forYearMonth(yearMonth: YearMonth): InstantRange = {
    val start = LocalDateTime.of(yearMonth.atDay(1), LocalTime.MIN).toInstant(ZoneOffset.UTC)
    val end   = LocalDateTime.of(yearMonth.atDay(1).plusMonths(1), LocalTime.MIN).toInstant(ZoneOffset.UTC)
    InstantRange(start, end)
  }

}
