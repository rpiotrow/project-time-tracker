package io.github.rpiotrow.ptt.gateway.web

import akka.http.scaladsl.model.StatusCodes.{MethodNotAllowed, NotFound}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{RawHeader, Authorization => AkkaAuthorizationHeader}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{RejectionHandler, RequestContext, Route, RouteResult}
import io.github.rpiotrow.ptt.api.allEndpointsWithAuth
import io.github.rpiotrow.ptt.api.model.UserId
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.swagger.akkahttp.SwaggerAkka

import scala.concurrent.Future

object Routes {
  val docsRoute: Route = {
    val openapi = allEndpointsWithAuth.toOpenAPI("Project Time Tracker", "1.0")
    new SwaggerAkka(openapi.toYaml).routes
  }

  def serviceRoute(authorization: Authorization, readSideProxy: ServiceProxy, writeSideProxy: ServiceProxy): Route = {
    Route {
      handleRejections(jsonRejections) {
        authenticateOAuth2[UserId](realm = "ptt", jwtAuthenticator(authorization)) { userId => context =>
          val uriPath = context.request.uri.path.toString()

          if (uriPath.startsWith("/projects")) {
            handleProjectsRequest(context, userId, readSideProxy, writeSideProxy)
          } else if (uriPath.startsWith("/statistics")) {
            handleStatisticsRequest(context, readSideProxy)
          } else {
            context.complete(NotFound, jsonError("Resource not found"))
          }
        }
      }
    }
  }

  private def jwtAuthenticator(authorization: Authorization)(credentials: Credentials): Option[UserId] =
    credentials match {
      case Credentials.Provided(rawToken) => authorization.getUserId(rawToken)
      case _                              => None
    }

  private def jsonRejections =
    RejectionHandler.default
      .mapRejectionResponse {
        case res @ HttpResponse(_, _, ent: HttpEntity.Strict, _) =>
          // since all Akka default rejection responses are Strict this will handle all rejections
          val message = ent.data.utf8String.replaceAll("\"", """\"""")
          // we copy the response in order to keep all headers and status code, wrapping the message as hand rolled JSON
          res.withEntity(jsonError(message))

        // pass through all other types of responses
        case x                                                   => x
      }

  private val writeMethods = List(HttpMethods.POST, HttpMethods.PUT, HttpMethods.DELETE)

  private def handleProjectsRequest(
    context: RequestContext,
    userId: UserId,
    readSideProxy: ServiceProxy,
    writeSideProxy: ServiceProxy
  ): Future[RouteResult] = {
    if (context.request.method == HttpMethods.GET) {
      val eventualResponse = readSideProxy.queueRequest(context.request)
      context.complete(eventualResponse)
    } else if (writeMethods.contains(context.request.method)) {
      context.complete(
        writeSideProxy.queueRequest(context.request.addHeader(RawHeader("X-Authorization", userId.id.toString)))
      )
    } else {
      context.complete(MethodNotAllowed, jsonError("HTTP method not allowed, supported methods: GET,PUT,POST,DELETE"))
    }
  }

  private def handleStatisticsRequest(context: RequestContext, readSideProxy: ServiceProxy): Future[RouteResult] = {
    if (context.request.method == HttpMethods.GET) {
      context.complete(readSideProxy.queueRequest(context.request))
    } else {
      context.complete(MethodNotAllowed, jsonError("HTTP method not allowed, supported methods: GET"))
    }
  }

  private def jsonError(message: String) = HttpEntity(ContentTypes.`application/json`, s"""{"error": "$message"}""")

}
