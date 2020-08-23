package io.github.rpiotrow.ptt

import java.time.format.DateTimeParseException
import java.time.{LocalDateTime, YearMonth}
import java.net.URL

import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.refined._
import io.github.rpiotrow.ptt.api.error._
import io.github.rpiotrow.ptt.api.model._
import io.github.rpiotrow.ptt.api.input._
import io.github.rpiotrow.ptt.api.output._
import io.github.rpiotrow.ptt.api.param._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.codec.enumeratum._
import sttp.tapir.codec.refined._
import sttp.tapir.json.circe._
import cats.implicits._
import eu.timepit.refined.auto._
import io.github.rpiotrow.ptt.api.input.{ProjectInput, TaskInput}
import io.github.rpiotrow.ptt.api.output.{ProjectOutput, StatisticsOutput}
import io.github.rpiotrow.ptt.api.param.{OrderingDirection, ProjectListParams, ProjectOrderingField, StatisticsParams}
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.integ.cats.codec._

package object api {

  private val baseEndpoint     = endpoint
    .errorOut(
      oneOf[ApiError](
        statusMapping(StatusCode.NotFound, jsonBody[NotFound]),
        statusMapping(StatusCode.Unauthorized, emptyOutput.map(_ => Unauthorized)(_ => ())),
        statusMapping(StatusCode.BadRequest, jsonBody[InvalidInput]),
        statusMapping(StatusCode.Conflict, jsonBody[Conflict]),
        statusMapping(StatusCode.Forbidden, jsonBody[Forbidden]),
        statusMapping(StatusCode.InternalServerError, jsonBody[ServerError].description("server error"))
      )
    )
  private val projectsEndpoint = baseEndpoint
    .in("projects")

  private val defaultPageNumber: PageNumber = 0
  private val defaultPageSize: PageSize     = 25

  private val pageNumberInput: EndpointInput[PageNumber] =
    query[Option[PageNumber]]("pageNumber")
      .description(s"Page number ($defaultPageNumber if not provided)")
      .map(Mapping.from[Option[PageNumber], PageNumber] { pageNumberOption: Option[PageNumber] =>
        pageNumberOption.getOrElse(defaultPageNumber)
      } { pageNumber =>
        pageNumber.some
      })
  private val pageSizeInput: EndpointInput[PageSize]     =
    query[Option[PageSize]]("pageSize")
      .description(s"Page size ($defaultPageSize if not provided)")
      .map(Mapping.from[Option[PageSize], PageSize] { pageSizeOption: Option[PageSize] =>
        pageSizeOption.getOrElse(defaultPageSize)
      } { pageSize =>
        pageSize.some
      })

  private val projectListInput: EndpointInput[ProjectListParams] =
    query[List[ProjectId]]("ids")
      .and(query[Option[LocalDateTime]]("from"))
      .and(query[Option[LocalDateTime]]("to"))
      .and(query[Option[Boolean]]("deleted"))
      .and(query[Option[ProjectOrderingField]]("orderBy"))
      .and(query[Option[OrderingDirection]]("orderDirection"))
      .and(pageNumberInput)
      .and(pageSizeInput)
      .mapTo(ProjectListParams)

  val projectListEndpoint = projectsEndpoint.get
    .in(projectListInput)
    .out(jsonBody[List[ProjectOutput]])

  val projectDetailEndpoint = projectsEndpoint
    .in(path[ProjectId]("id"))
    .get
    .out(jsonBody[ProjectOutput])

  private def urlDecode(urlString: String): DecodeResult[URL]  =
    try { DecodeResult.Value(new URL(urlString)) }
    catch { case e: Exception => DecodeResult.Error(urlString, e) }
  private def urlEncode(url: URL): String                      =
    url.toString
  implicit private val urlCodec: Codec[String, URL, TextPlain] =
    Codec.string.mapDecode(urlDecode)(urlEncode)

  val projectCreateEndpoint = projectsEndpoint.post
    .in(jsonBody[ProjectInput])
    .out(header[LocationHeader]("location"))
    .out(statusCode(StatusCode.Created))

  val projectUpdateEndpoint = projectsEndpoint
    .in(path[ProjectId]("id"))
    .put
    .in(jsonBody[ProjectInput])
    .out(header[LocationHeader]("location"))
    .out(statusCode(StatusCode.Ok))

  val projectDeleteEndpoint = projectsEndpoint
    .in(path[ProjectId]("id"))
    .delete
    .out(statusCode(StatusCode.Ok))

  private val tasksEndpoint = projectsEndpoint
    .in(path[ProjectId]("id"))
    .in("tasks")

  val taskCreateEndpoint = tasksEndpoint.post
    .in(jsonBody[TaskInput])
    .out(header[LocationHeader]("location"))
    .out(statusCode(StatusCode.Created))

  val taskUpdateEndpoint = tasksEndpoint
    .in(path[TaskId]("id"))
    .put
    .in(jsonBody[TaskInput])
    .out(header[LocationHeader]("location"))
    .out(statusCode(StatusCode.Ok))

  val taskDeleteEndpoint = tasksEndpoint
    .in(path[TaskId]("id"))
    .delete
    .out(statusCode(StatusCode.Ok))

  private def yearMonthDecode(yearMonthString: String): DecodeResult[YearMonth] =
    try { DecodeResult.Value(YearMonth.parse(yearMonthString)) }
    catch { case e: DateTimeParseException => DecodeResult.Error(yearMonthString, e) }
  private def yearMonthEncode(yearMonth: YearMonth): String                     =
    yearMonth.toString
  implicit private val yearMonthCodec: Codec[String, YearMonth, TextPlain]      =
    Codec.string.mapDecode(yearMonthDecode)(yearMonthEncode)

  private val statisticsInput: EndpointInput[StatisticsParams] =
    query[NonEmptyUserIdList]("ids")
      .and(query[YearMonth]("from"))
      .and(query[YearMonth]("to"))
      .mapTo(StatisticsParams)
      .validate(Validator.custom(input => !input.to.isBefore(input.from), "`from` before or equal `to`"))

  val statisticsEndpoint = baseEndpoint
    .in("statistics")
    .get
    .in(statisticsInput)
    .out(jsonBody[StatisticsOutput])

  import scala.language.existentials
  val allEndpointsWithAuth = List(
    projectListEndpoint,
    projectDetailEndpoint,
    projectCreateEndpoint,
    projectUpdateEndpoint,
    projectDeleteEndpoint,
    taskCreateEndpoint,
    taskUpdateEndpoint,
    taskDeleteEndpoint,
    statisticsEndpoint
  ).map(_.in(auth.bearer[BearerToken]))

}
