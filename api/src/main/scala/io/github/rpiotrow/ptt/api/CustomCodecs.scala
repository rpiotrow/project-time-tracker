package io.github.rpiotrow.ptt.api

import java.net.URL
import java.time.YearMonth
import java.time.format.DateTimeParseException

import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir._

private[api] object CustomCodecs {

  private def urlDecode(urlString: String): DecodeResult[URL] =
    try { DecodeResult.Value(new URL(urlString)) }
    catch { case e: Exception => DecodeResult.Error(urlString, e) }
  private def urlEncode(url: URL): String                     =
    url.toString
  implicit val urlCodec: Codec[String, URL, TextPlain]        =
    Codec.string.mapDecode(urlDecode)(urlEncode)

  private def yearMonthDecode(yearMonthString: String): DecodeResult[YearMonth] =
    try { DecodeResult.Value(YearMonth.parse(yearMonthString)) }
    catch { case e: DateTimeParseException => DecodeResult.Error(yearMonthString, e) }
  private def yearMonthEncode(yearMonth: YearMonth): String                     =
    yearMonth.toString
  implicit val yearMonthCodec: Codec[String, YearMonth, TextPlain]              =
    Codec.string.mapDecode(yearMonthDecode)(yearMonthEncode)

}
