package io.github.rpiotrow.ptt.api

import java.time.OffsetDateTime

import eu.timepit.refined.auto._
import io.circe.generic.auto._
import io.circe.refined._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.codec.enumeratum._
import sttp.tapir.codec.refined._
import sttp.tapir.json.circe._
import cats.implicits._
import io.github.rpiotrow.ptt.api.input._
import io.github.rpiotrow.ptt.api.model._
import io.github.rpiotrow.ptt.api.output._
import io.github.rpiotrow.ptt.api.param.OrderingDirection._
import io.github.rpiotrow.ptt.api.param.ProjectOrderingField._
import io.github.rpiotrow.ptt.api.param._
import sttp.tapir.EndpointIO.Example

object ProjectEndpoints {

  import Base._
  import CirceMappings._
  import TapirMappings._
  import Paging._

  private[api] val projectsBaseEndpoint = baseEndpoint
    .in("projects")

  private[api] val projectWithIdBaseEndpoint = projectsBaseEndpoint
    .in(path[ProjectId]("projectId").description("project identifier").example(ProjectId.example))

  private val projectListInput: EndpointInput[ProjectListParams] =
    query[List[ProjectId]]("ids")
      .description("Filter projects with given identifiers")
      .example(ProjectId.exampleList)
      .and(
        query[Option[OffsetDateTime]]("from")
          .description("Filter projects created after given date")
          .example(OffsetDateTime.parse("2020-01-01T10:00:00Z").some)
      )
      .and(
        query[Option[OffsetDateTime]]("to")
          .description("Filter projects created before given date")
          .example(OffsetDateTime.parse("2020-12-31T10:00:00Z").some)
      )
      .and(
        query[Option[Boolean]]("deleted")
          .description("Filter deleted or not deleted projects")
          .examples(
            List(Example(true.some, "only deleted".some, None), Example(false.some, "only not deleted".some, None))
          )
      )
      .and(
        query[Option[ProjectOrderingField]]("orderBy")
          .description(
            "Sort projects by create date or last add duration date (date of the last added task or create date for empty projects)"
          )
          .examples(
            List(
              Example(CreatedAt.some, "by create date".some, None),
              Example(LastAddDurationAt.some, "by last add duration date".some, None)
            )
          )
      )
      .and(
        query[Option[OrderingDirection]]("orderDirection")
          .description("Order of sorting")
          .examples(
            List(
              Example(Ascending.some, "ascending order".some, None),
              Example(Descending.some, "descending order".some, None)
            )
          )
      )
      .and(pageNumberInput)
      .and(pageSizeInput)
      .mapTo(ProjectListParams)

  val projectListEndpoint: Endpoint[ProjectListParams, error.ApiError, List[ProjectOutput], Nothing] =
    projectsBaseEndpoint.get
      .in(projectListInput)
      .out(jsonBody[List[ProjectOutput]].example(List(ProjectOutput.example)))

  val projectDetailEndpoint: Endpoint[ProjectId, error.ApiError, ProjectOutput, Nothing] =
    projectWithIdBaseEndpoint.get
      .out(jsonBody[ProjectOutput].example(ProjectOutput.example))

  val projectCreateEndpoint: Endpoint[ProjectInput, error.ApiError, LocationHeader, Nothing] =
    projectsBaseEndpoint.post
      .in(jsonBody[ProjectInput].example(ProjectInput.example))
      .out(
        header[LocationHeader]("location")
          .description("URI of created project")
          .example(LocationHeader.exampleProjectLocation)
      )
      .out(statusCode(StatusCode.Created))

  val projectUpdateEndpoint: Endpoint[(ProjectId, ProjectInput), error.ApiError, LocationHeader, Nothing] =
    projectWithIdBaseEndpoint.put
      .in(jsonBody[ProjectInput].example(ProjectInput.example))
      .out(
        header[LocationHeader]("location")
          .description("URI of updated project")
          .example(LocationHeader.exampleProjectLocation)
      )
      .out(statusCode(StatusCode.Ok))

  val projectDeleteEndpoint: Endpoint[ProjectId, error.ApiError, Unit, Nothing] =
    projectWithIdBaseEndpoint.delete
      .out(statusCode(StatusCode.Ok))

}
