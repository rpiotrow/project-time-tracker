package io.github.rpiotrow.ptt.write.service

import java.time.{Duration, LocalDateTime, LocalTime, Period, YearMonth}

case class LocalDateTimeRange(from: LocalDateTime, to: LocalDateTime) {
  lazy val fromYearMonth: YearMonth = YearMonth.of(from.getYear, from.getMonth)
  lazy val endYearMonth: YearMonth  = YearMonth.of(to.getYear, to.getMonth)

  def intersection(other: LocalDateTimeRange): Option[LocalDateTimeRange] = {
    val start = if (this.from.isAfter(other.from)) this.from else other.from
    val end   = if (this.to.isBefore(other.to)) this.to else other.to
    if (start.isBefore(end))
      Some(LocalDateTimeRange(start, end))
    else
      None
  }

  def duration(): Duration = Duration.between(from, to)

  def splitToMonths(): List[LocalDateTimeRange] = {
    if (fromYearMonth == endYearMonth) {
      List(this)
    } else {
      val months = Period.between(fromYearMonth.atDay(1), endYearMonth.atDay(1)).getMonths
      (0 to months).map(i => LocalDateTimeRange.forYearMonth(fromYearMonth.plusMonths(i)).intersection(this).get).toList
    }
  }
}

object LocalDateTimeRange {

  def forYearMonth(yearMonth: YearMonth): LocalDateTimeRange = {
    val start = LocalDateTime.of(yearMonth.atDay(1), LocalTime.MIN)
    val end   = LocalDateTime.of(yearMonth.atDay(1).plusMonths(1), LocalTime.MIN)
    LocalDateTimeRange(start, end)
  }

}
