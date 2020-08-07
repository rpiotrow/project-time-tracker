package io.github.rpiotrow.ptt.gateway.web

import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.StatusCodes.{MethodNotAllowed, NotFound}
import akka.http.scaladsl.server.Route
import io.github.rpiotrow.ptt.api.allEndpointsWithAuth
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.swagger.akkahttp.SwaggerAkka

object Routes {
  val docsRoute: Route = {
    val openapi = allEndpointsWithAuth.toOpenAPI("Project Time Tracker", "1.0")
    new SwaggerAkka(openapi.toYaml).routes
  }

  def serviceRoute(authorization: Authorization, readSideProxy: ServiceProxy, writeSideProxy: ServiceProxy): Route = {
    val writeMethods = List(HttpMethods.POST, HttpMethods.PUT, HttpMethods.DELETE)
    Route {
      authorization.check { _ => context =>
        val uriPath = context.request.uri.path.toString()

        def handleProjectsRequest = {
          if (context.request.method == HttpMethods.GET) {
            context.complete(readSideProxy.queueRequest(context.request))
          } else if (writeMethods.contains(context.request.method)) {
            context.complete(writeSideProxy.queueRequest(context.request))
          } else {
            context.complete(MethodNotAllowed)
          }
        }

        def handleStatisticsRequest = {
          if (context.request.method == HttpMethods.GET) {
            context.complete(readSideProxy.queueRequest(context.request))
          } else {
            context.complete(MethodNotAllowed)
          }
        }

        if (uriPath.startsWith("/projects")) {
          handleProjectsRequest
        } else if (uriPath.startsWith("/statistics")) {
          handleStatisticsRequest
        } else {
          context.complete(NotFound)
        }
      }
    }
  }
}
