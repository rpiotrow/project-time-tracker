package io.github.rpiotrow.ptt.api

import io.circe.generic.auto._
import io.github.rpiotrow.ptt.api.error._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._

private[api] object Base {

  val baseEndpoint = endpoint
    .errorOut(
      oneOf[ApiError](
        statusMapping(StatusCode.NotFound, jsonBody[NotFound].description("Resource not found")),
        statusMapping(StatusCode.Unauthorized, emptyOutput.map(_ => Unauthorized)(_ => ())),
        statusMapping(
          StatusCode.BadRequest,
          jsonBody[InvalidInput].description("Validation of the request was not successful")
        ),
        statusMapping(StatusCode.Conflict, jsonBody[Conflict].description("There was conflict with current state")),
        statusMapping(
          StatusCode.Forbidden,
          jsonBody[Forbidden].description("User is not authorized to perform action")
        ),
        statusMapping(StatusCode.InternalServerError, jsonBody[ServerError].description("server error"))
      )
    )

}
