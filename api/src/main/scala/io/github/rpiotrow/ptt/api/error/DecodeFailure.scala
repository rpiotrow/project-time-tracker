package io.github.rpiotrow.ptt.api.error

import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.{DecodeFailureHandling, ServerDefaults}
import sttp.tapir.statusCode

case class DecodeFailure(message: String)

object DecodeFailure {

  def failureResponse(code: StatusCode, message: String): DecodeFailureHandling =
    DecodeFailureHandling.response(statusCode.and(jsonBody[DecodeFailure]))((code, DecodeFailure(message)))

  val decodeFailureHandler = ServerDefaults.decodeFailureHandler.copy(response = failureResponse)

}
