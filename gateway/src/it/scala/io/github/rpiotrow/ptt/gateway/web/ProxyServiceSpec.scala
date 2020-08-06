package io.github.rpiotrow.ptt.gateway.web

import akka.actor.ActorSystem
import akka.http.scaladsl.client.RequestBuilding.{Get, Post}
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.scalatest.funspec.FixtureAsyncFunSpec
import org.scalatest.matchers.should

import scala.concurrent.ExecutionContextExecutor

class ProxyServiceSpec extends FixtureAsyncFunSpec with should.Matchers {

  private val port = 1080
  type FixtureParam = ClientAndServer

  def withFixture(test: OneArgAsyncTest) = {
    val mockServer = startClientAndServer(port)
    complete {
      withFixture(test.toNoArgAsyncTest(mockServer))
    } lastly {
      mockServer.stop()
    }
  }

  describe("ProxyService") {
    it("should proxy successful request") { mockServer =>
      mockGetStatistics(mockServer)

      implicit val system: ActorSystem          = ActorSystem("ProxyTest")
      implicit val ec: ExecutionContextExecutor = system.dispatcher
      val serviceProxy                          = new ServiceProxyLive("localhost", port)

      serviceProxy.queueRequest(Get("http://localhost:8080/statistics")) map { _.status should be(StatusCodes.OK) }
    }
    it("should proxy error request") { mockServer =>
      mockServer
        .when(request().withMethod("POST").withPath("/projects"))
        .respond(response().withStatusCode(418))

      implicit val system: ActorSystem          = ActorSystem("ProxyTest")
      implicit val ec: ExecutionContextExecutor = system.dispatcher
      val serviceProxy                          = new ServiceProxyLive("localhost", port)

      serviceProxy.queueRequest(Post("http://localhost:8080/projects")) map {
        _.status should be(StatusCodes.ImATeapot)
      }
    }
    it("should return 503 when service is not available") { () =>
      implicit val system: ActorSystem          = ActorSystem("ProxyTest")
      implicit val ec: ExecutionContextExecutor = system.dispatcher
      val serviceProxy                          = new ServiceProxyLive("localhost", port + 1)

      serviceProxy.queueRequest(Post("http://localhost:8080/projects")) map {
        _.status should be(StatusCodes.ServiceUnavailable)
      }
    }
    it("should return proper answer when multiple requests are sent") { mockServer =>
      mockGetStatistics(mockServer)
      mockCreateProject(mockServer)

      implicit val system: ActorSystem          = ActorSystem("ProxyTest")
      implicit val ec: ExecutionContextExecutor = system.dispatcher
      val serviceProxy                          = new ServiceProxyLive("localhost", port)

      serviceProxy.queueRequest(Get("http://localhost:8080/statistics")) map { _.status should be(StatusCodes.OK) }
      serviceProxy.queueRequest(Post("http://localhost:8080/projects", HttpEntity(createProjectBody))) map {
        _.status should be(StatusCodes.Created)
      }
      serviceProxy.queueRequest(Get("http://localhost:8080/statistics")) map { _.status should be(StatusCodes.OK) }
    }
  }

  private val createProjectBody = """{"id":"one"}"""

  private def mockCreateProject(mockServer: ClientAndServer) = {
    mockServer
      .when(request().withMethod("POST").withPath("/projects").withBody(createProjectBody))
      .respond(response().withStatusCode(201).withHeader("Location", "http://localhost:1080/projects/one"))
  }

  private def mockGetStatistics(mockServer: ClientAndServer) = {
    mockServer
      .when(request().withMethod("GET").withPath("/statistics"))
      .respond(response().withStatusCode(200))
  }
}
