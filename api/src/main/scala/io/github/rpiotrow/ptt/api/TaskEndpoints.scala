package io.github.rpiotrow.ptt.api

import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import io.circe.generic.auto._
import io.github.rpiotrow.ptt.api.ProjectEndpoints.projectWithIdBaseEndpoint
import io.github.rpiotrow.ptt.api.input._
import io.github.rpiotrow.ptt.api.model._
import io.github.rpiotrow.ptt.api.output._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.codec.refined._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

object TaskEndpoints {

  import CirceMappings._
  import TapirMappings._

  private val tasksBaseEndpoint      = projectWithIdBaseEndpoint
    .in("tasks")
  private val taskWithIdBaseEndpoint = tasksBaseEndpoint
    .in(path[TaskId]("id").description("task identifier").example(TaskId.example))

  val taskCreateEndpoint: Endpoint[(ProjectId, TaskInput), error.ApiError, LocationHeader, Any] =
    tasksBaseEndpoint.post
      .in(jsonBody[TaskInput].example(TaskInput.example))
      .out(header[LocationHeader]("location").example(LocationHeader.exampleTaskLocation))
      .out(statusCode(StatusCode.Created))

  val taskUpdateEndpoint: Endpoint[(ProjectId, TaskId, TaskInput), error.ApiError, LocationHeader, Any] =
    taskWithIdBaseEndpoint.put
      .in(jsonBody[TaskInput].example(TaskInput.example))
      .out(header[LocationHeader]("location").example(LocationHeader.exampleTaskLocation))
      .out(statusCode(StatusCode.Ok))

  val taskDeleteEndpoint: Endpoint[(ProjectId, TaskId), error.ApiError, Unit, Any] =
    taskWithIdBaseEndpoint.delete
      .out(statusCode(StatusCode.Ok))

}
