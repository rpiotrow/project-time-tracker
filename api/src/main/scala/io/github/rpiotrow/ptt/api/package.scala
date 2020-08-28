package io.github.rpiotrow.ptt

import io.github.rpiotrow.ptt.api.input._
import sttp.tapir._
import sttp.tapir.codec.refined._

package object api {

  import ProjectEndpoints._
  import StatisticsEndpoints._
  import TaskEndpoints._

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
