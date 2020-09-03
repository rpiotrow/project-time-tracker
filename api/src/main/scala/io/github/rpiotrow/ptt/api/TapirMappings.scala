package io.github.rpiotrow.ptt.api

import java.net.URL
import java.time.YearMonth
import java.time.format.DateTimeParseException

import io.github.rpiotrow.ptt.api.model._
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir._
import sttp.tapir.integ.cats.codec._

object TapirMappings {

  private def urlDecode(urlString: String): DecodeResult[URL] =
    try { DecodeResult.Value(new URL(urlString)) }
    catch { case e: Exception => DecodeResult.Error(urlString, e) }
  implicit val urlCodec: Codec[String, URL, TextPlain]        =
    Codec.string.mapDecode(urlDecode)(_.toString)

  private def yearMonthDecode(yearMonthString: String): DecodeResult[YearMonth] =
    try { DecodeResult.Value(YearMonth.parse(yearMonthString)) }
    catch { case e: DateTimeParseException => DecodeResult.Error(yearMonthString, e) }
  implicit val yearMonthCodec: Codec[String, YearMonth, TextPlain]              =
    Codec.string.mapDecode(yearMonthDecode)(_.toString)

  implicit val userIdCodec: Codec[String, UserId, TextPlain] =
    Codec.uuid.mapDecode(v => DecodeResult.Value(UserId(v)))(_.id)

  implicit val taskIdCodec: Codec[String, TaskId, TextPlain] =
    Codec.uuid.mapDecode(v => DecodeResult.Value(TaskId(v)))(_.id)

  implicit val nonEmptyUserIdListCodec: Codec[List[String], NonEmptyUserIdList, TextPlain] =
    codecForNonEmptyList[List[String], UserId, TextPlain]
      .mapDecode(v => DecodeResult.Value(NonEmptyUserIdList(v)))(_.list)

}
