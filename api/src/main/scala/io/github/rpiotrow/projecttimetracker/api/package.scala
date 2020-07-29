package io.github.rpiotrow.projecttimetracker

import java.time.LocalDateTime

import io.circe.generic.auto._
import io.circe.refined._
import io.github.rpiotrow.projecttimetracker.api.Errors._
import io.github.rpiotrow.projecttimetracker.api.Model._
import io.github.rpiotrow.projecttimetracker.api.input._
import io.github.rpiotrow.projecttimetracker.api.output._
import io.github.rpiotrow.projecttimetracker.api.param._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.codec.enumeratum._
import sttp.tapir.codec.refined._
import sttp.tapir.json.circe._

package object api {

  private val baseEndpoint = endpoint
    .in("projects")
    .errorOut(
      oneOf[ApiError](
        statusMapping(StatusCode.NotFound, emptyOutput.map(_ => NotFound)(_ => ())),
        statusMapping(StatusCode.Unauthorized, emptyOutput.map(_ => Unauthorized)(_ => ())),
        statusMapping(StatusCode.BadRequest, jsonBody[InputNotValid]),
        statusMapping(StatusCode.InternalServerError, jsonBody[ServerError].description("server error"))
      )
    )

  val projectListEndpoint = baseEndpoint.get
    .in(query[List[ProjectId]]("ids"))
    .in(query[Option[LocalDateTime]]("from"))
    .in(query[Option[LocalDateTime]]("to"))
    .in(query[Option[Boolean]]("deleted"))
    .in(query[Option[ProjectOrderingField]]("orderBy"))
    .in(query[Option[OrderingDirection]]("orderDirection"))
    .in(query[Option[PageNumber]]("pageNumber"))
    .in(query[Option[PageSize]]("pageSize"))
    .out(jsonBody[List[ProjectOutput]])

  val projectDetailEndpoint = baseEndpoint
    .in(path[ProjectId]("id"))
    .get
    .out(jsonBody[ProjectOutput])

  val projectCreateEndpoint = baseEndpoint.post
    .in(jsonBody[ProjectInput])
    .out(header[LocationHeader]("location"))
    .out(statusCode(StatusCode.Created))

  val projectUpdateEndpoint = baseEndpoint
    .in(path[ProjectId]("id"))
    .put
    .in(jsonBody[ProjectInput])
    .out(header[LocationHeader]("location"))
    .out(statusCode(StatusCode.Ok))

  val projectDeleteEndpoint = baseEndpoint
    .in(path[ProjectId]("id"))
    .delete
    .out(statusCode(StatusCode.Ok))

  val taskCreateEndpoint = baseEndpoint
    .in(path[ProjectId]("id"))
    .in("task")
    .post
    .in(jsonBody[TaskInput])
    .out(header[LocationHeader]("location"))
    .out(statusCode(StatusCode.Created))

  val taskUpdateEndpoint = baseEndpoint
    .in(path[ProjectId]("id"))
    .in("task")
    .in(path[TaskId]("id"))
    .put
    .in(jsonBody[TaskInput])
    .out(header[LocationHeader]("location"))
    .out(statusCode(StatusCode.Created))

  val taskDeleteEndpoint = baseEndpoint
    .in(path[ProjectId]("id"))
    .in("task")
    .in(path[TaskId]("id"))
    .delete
    .out(statusCode(StatusCode.Ok))

  val statisticsEndpoint = baseEndpoint
    .in("statistics")
    .get
    .in(query[List[UserId]]("ids"))
    .in(query[Option[LocalDateTime]]("from")) // TODO: new type for format YYYY-MM
    .in(query[Option[LocalDateTime]]("to"))   // TODO: new type for format YYYY-MM
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
