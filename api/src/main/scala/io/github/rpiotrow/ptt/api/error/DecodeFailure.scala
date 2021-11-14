package io.github.rpiotrow.ptt.api.error

import io.circe.generic.auto._
import sttp.model.{Header, StatusCode}
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.interceptor.ValuedEndpointOutput
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler

case class DecodeFailure(message: String)

object DecodeFailure {

  val decodeFailureHandler: DefaultDecodeFailureHandler =
    DefaultDecodeFailureHandler.handler.copy(response = failureResponse)

  private def failureResponse(code: StatusCode, headers: List[Header], message: String): ValuedEndpointOutput[_] =
    ValuedEndpointOutput(jsonBody[DecodeFailure], DecodeFailure(message))

}
