package io.github.rpiotrow.ptt.gateway.web

import java.util.UUID

import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives.provide
import akka.http.scaladsl.server.{Directive1, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.github.rpiotrow.ptt.api.model.UserId
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

import scala.concurrent.Future

class RoutesSpec extends AnyFunSpec with MockFactory with ScalatestRouteTest with should.Matchers {

  describe("Routes") {
    describe("/projects") {
      describe("read-side") {
        it("GET") {
          checkRouteViaProxy(Get("http://localhost:8080/projects"))
        }
      }
      describe("write-side") {
        it("POST") {
          checkRouteViaProxy(Post("http://localhost:8080/projects"))
        }
        it("DELETE") {
          checkRouteViaProxy(Delete("http://localhost:8080/projects/one"))
        }
        it("PUT") {
          checkRouteViaProxy(Put("http://localhost:8080/projects/one"))
        }
      }
      describe("not allowed") {
        it("PATCH") {
          checkWithStatus(Patch("/projects"), StatusCodes.MethodNotAllowed)
        }
      }
    }
    describe("/statistics") {
      describe("read-side") {
        it("GET") {
          checkRouteViaProxy(Get("http://localhost:8080/statistics"))
        }
      }
      describe("not allowed") {
        it("POST") {
          checkWithStatus(Post("/statistics"), StatusCodes.MethodNotAllowed)
        }
        it("DELETE") {
          checkWithStatus(Delete("/statistics"), StatusCodes.MethodNotAllowed)
        }
        it("PUT") {
          checkWithStatus(Put("/statistics"), StatusCodes.MethodNotAllowed)
        }
        it("PATCH") {
          checkWithStatus(Patch("/statistics"), StatusCodes.MethodNotAllowed)
        }
      }
    }
    describe("other") {
      it("should return not found") {
        checkWithStatus(Get("/ufo"), StatusCodes.NotFound)
      }
    }
  }

  private val noOpAuthorization = new Authorization {
    override def check: Directive1[UserId] = provide(UUID.randomUUID())
  }

  private def noOpRoutes(): Route = Routes.serviceRoute(noOpAuthorization, mock[ServiceProxy], mock[ServiceProxy])

  private def checkRouteViaProxy(request: HttpRequest) = {
    val readSide  = mock[ServiceProxy]
    val writeSide = mock[ServiceProxy]
    val proxy     = if (request.method == HttpMethods.GET) readSide else writeSide

    (proxy.queueRequest _)
      .expects(request)
      .returning(Future.successful(HttpResponse(entity = HttpEntity("test"))))
    val routes = Routes.serviceRoute(noOpAuthorization, readSide, writeSide)

    request ~> routes ~> check {
      status shouldEqual StatusCodes.OK
      entityAs[String] shouldEqual "test"
    }
  }

  private def checkWithStatus(request: HttpRequest, expectedStatus: StatusCode) = {
    request ~> noOpRoutes() ~> check {
      status shouldEqual expectedStatus
    }
  }
}
