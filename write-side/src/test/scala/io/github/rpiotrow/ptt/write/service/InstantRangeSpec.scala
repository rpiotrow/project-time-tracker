package io.github.rpiotrow.ptt.write.service

import java.time.{Duration, Instant, LocalDateTime, YearMonth, ZoneOffset}

import cats.implicits._
import com.softwaremill.diffx.generic.auto._
import com.softwaremill.diffx.scalatest.DiffMatcher._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

class InstantRangeSpec extends AnyFunSpec with should.Matchers {

  describe("InstantRange") {
    describe("intersection") {
      it("should return intersection for overlapping ranges") {
        val r1 = createRange("2020-08-01T10:00:00", "2020-08-10T17:00:00")
        val r2 = createRange("2020-08-05T10:00:00", "2020-08-10T18:00:00")

        r1.intersection(r2) should matchTo(createRange("2020-08-05T10:00:00", "2020-08-10T17:00:00").some)
      }
      it("should return none for not overlapping ranges") {
        val r1 = createRange("2020-08-01T10:00:00", "2020-08-10T17:00:00")
        val r2 = createRange("2020-08-10T17:00:00", "2020-08-10T23:00:00")

        r1.intersection(r2) should be(None)
      }
    }

    describe("duration") {
      it("should return duration between days") {
        val duration = createRange("2020-08-01T23:00:00", "2020-08-02T03:00:00").duration()

        duration should be(Duration.ofHours(4))
      }
    }

    describe("splitToMonths") {
      it("should return one element when range is within one month") {
        val months = createRange("2020-08-01T10:00:00", "2020-08-10T10:00:00").splitToMonths()

        months should matchTo(List(createRange("2020-08-01T10:00:00", "2020-08-10T10:00:00")))
      }
      it("should return 3 elements when range is within on3 month") {
        val months = createRange("2020-08-30T00:00:00", "2020-10-10T00:00:00").splitToMonths()

        months should matchTo(
          List(
            createRange("2020-08-30T00:00:00", "2020-09-01T00:00:00"),
            createRange("2020-09-01T00:00:00", "2020-10-01T00:00:00"),
            createRange("2020-10-01T00:00:00", "2020-10-10T00:00:00")
          )
        )
      }
    }

    describe("forYearMonth") {
      it("should return month range") {
        val range = InstantRange.forYearMonth(YearMonth.of(2020, 8))

        (range.from, range.to) should be((Instant.parse("2020-08-01T00:00:00Z"), Instant.parse("2020-09-01T00:00:00Z")))
      }
    }
  }

  private def createRange(from: String, to: String) = {
    val fromDate = LocalDateTime.parse(from).toInstant(ZoneOffset.UTC)
    val toDate   = LocalDateTime.parse(to).toInstant(ZoneOffset.UTC)
    InstantRange(fromDate, toDate)
  }

}
