package io.github.rpiotrow.ptt.gateway.web

import java.time.Instant
import java.util.UUID

import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpEntity, StatusCodes, _}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.MissingHeaderRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.github.rpiotrow.ptt.gateway.configuration.AuthorizationConfig
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import sttp.model.HeaderNames

class AuthorizationSpec extends AnyFunSpec with ScalatestRouteTest with should.Matchers {

  val jwtKey    = "secretKey"
  val algorithm = JwtAlgorithm.HS256

  val authorization = new AuthorizationLive(AuthorizationConfig(jwtKey, algorithm.toString))
  val route         =
    path("hello") {
      get {
        authorization.check { _ =>
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      }
    }

  val claim = JwtClaim(
    expiration = Some(Instant.now.plusSeconds(120).getEpochSecond),
    issuedAt = Some(Instant.now.getEpochSecond),
    subject = Some(UUID.randomUUID().toString)
  )

  describe("Authorization") {
    it("should reject when token is not provided") {
      Get("/hello") ~> route ~> check {
        rejection shouldEqual MissingHeaderRejection(HeaderNames.Authorization)
      }
    }
    it("should return Unauthorized when token is invalid") {
      val invalidToken = JwtCirce.encode(claim, "differentKey", algorithm)
      Get("/hello").withHeaders(Authorization(OAuth2BearerToken(invalidToken))) ~> route ~> check {
        status should be(StatusCodes.Unauthorized)
      }
    }
    it("should return Unauthorized when token is expired") {
      val expiredClaim = JwtClaim(
        expiration = Some(Instant.now.minusSeconds(120).getEpochSecond),
        issuedAt = Some(Instant.now.minusSeconds(240).getEpochSecond),
        subject = Some(UUID.randomUUID().toString)
      )
      val expiredToken = JwtCirce.encode(expiredClaim, jwtKey, algorithm)
      Get("/hello").withHeaders(Authorization(OAuth2BearerToken(expiredToken))) ~> route ~> check {
        status should be(StatusCodes.Unauthorized)
      }
    }
    it("should return Unauthorized when token does not have subject") {
      val noSubjectClaim      = JwtClaim(
        expiration = Some(Instant.now.plusSeconds(120).getEpochSecond),
        issuedAt = Some(Instant.now.getEpochSecond)
      )
      val tokenWithoutSubject = JwtCirce.encode(noSubjectClaim, jwtKey, algorithm)
      Get("/hello").withHeaders(Authorization(OAuth2BearerToken(tokenWithoutSubject))) ~> route ~> check {
        status should be(StatusCodes.Unauthorized)
      }
    }
    it("should return OK when token is valid") {
      val claim = JwtClaim(
        expiration = Some(Instant.now.plusSeconds(120).getEpochSecond),
        issuedAt = Some(Instant.now.getEpochSecond),
        subject = Some(UUID.randomUUID().toString)
      )
      val token = JwtCirce.encode(claim, jwtKey, algorithm)
      Get("/hello").withHeaders(Authorization(OAuth2BearerToken(token))) ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }
  }
}
