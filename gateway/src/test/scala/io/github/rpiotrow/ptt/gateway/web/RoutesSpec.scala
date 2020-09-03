package io.github.rpiotrow.ptt.gateway.web

import java.util.UUID

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{OAuth2BearerToken, RawHeader, Authorization => AkkaAuthorizationHeader}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.github.rpiotrow.ptt.api.model.UserId
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should

import scala.concurrent.Future

class RoutesSpec extends AnyFunSpec with MockFactory with ScalatestRouteTest with should.Matchers {

  describe("Routes") {
    it("not authorized") {
      Get("http://localhost:8080/projects") ~> noOpRoutes() ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }
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
    describe("/ufo") {
      it("should return not found") {
        checkWithStatus(Get("/ufo"), StatusCodes.NotFound)
      }
    }
  }

  private val userIdUUID        = "67f410d5-9aaf-4186-8be5-f7e22740854c"
  private val userId            = UserId(userIdUUID)
  private val noOpAuthorization = new Authorization {
    override def getUserId(rawJwtToken: String): Option[UserId] =
      if (rawJwtToken == "token") Some(userId) else None
  }
  private val validCredentials  = OAuth2BearerToken("token")

  private def noOpRoutes(): Route = Routes.serviceRoute(noOpAuthorization, mock[ServiceProxy], mock[ServiceProxy])

  private def checkRouteViaProxy(request: HttpRequest) = {
    val readSide     = mock[ServiceProxy]
    val writeSide    = mock[ServiceProxy]
    val proxy        = if (request.method == HttpMethods.GET) readSide else writeSide
    val authRequest  = request.addHeader(AkkaAuthorizationHeader(OAuth2BearerToken("token")))
    val proxyRequest =
      if (request.method == HttpMethods.GET) authRequest
      else authRequest.addHeader(RawHeader("X-Authorization", userIdUUID))

    (proxy.queueRequest _)
      .expects(proxyRequest)
      .returning(Future.successful(HttpResponse(entity = HttpEntity("test"))))
    val routes = Routes.serviceRoute(noOpAuthorization, readSide, writeSide)

    authRequest ~> routes ~> check {
      status shouldEqual StatusCodes.OK
      entityAs[String] shouldEqual "test"
    }
  }

  private def checkWithStatus(request: HttpRequest, expectedStatus: StatusCode) = {
    request ~> addCredentials(validCredentials) ~> noOpRoutes() ~> check {
      status shouldEqual expectedStatus
    }
  }
}
