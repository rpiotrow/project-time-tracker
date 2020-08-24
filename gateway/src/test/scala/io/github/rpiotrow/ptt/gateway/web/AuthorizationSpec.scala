package io.github.rpiotrow.ptt.gateway.web

import java.time.Instant
import java.util.UUID

import io.github.rpiotrow.ptt.gateway.configuration.AuthorizationConfig
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

class AuthorizationSpec extends AnyFunSpec with should.Matchers {

  private val jwtKey    = "secretKey"
  private val algorithm = JwtAlgorithm.HS256

  private val authorization = new AuthorizationLive(AuthorizationConfig(jwtKey, algorithm.toString))

  private val userId = UUID.randomUUID()
  private val claim  = JwtClaim(
    expiration = Some(Instant.now.plusSeconds(120).getEpochSecond),
    issuedAt = Some(Instant.now.getEpochSecond),
    subject = Some(userId.toString)
  )

  describe("Authorization") {
    describe("getUserId should") {
      describe("return none") {
        it("when token is not provided") {
          authorization.getUserId("") should be(None)
        }
        it("when token is not valid") {
          authorization.getUserId("abc") should be(None)
        }
        it("when token is signed with different secret") {
          val invalidToken = JwtCirce.encode(claim, "differentKey", algorithm)
          authorization.getUserId(invalidToken) should be(None)
        }
        it("when token is expired") {
          val expiredClaim = JwtClaim(
            expiration = Some(Instant.now.minusSeconds(120).getEpochSecond),
            issuedAt = Some(Instant.now.minusSeconds(240).getEpochSecond),
            subject = Some(UUID.randomUUID().toString)
          )
          val expiredToken = JwtCirce.encode(expiredClaim, jwtKey, algorithm)
          authorization.getUserId(expiredToken) should be(None)
        }
        it("when token does not have subject") {
          val noSubjectClaim      = JwtClaim(
            expiration = Some(Instant.now.plusSeconds(120).getEpochSecond),
            issuedAt = Some(Instant.now.getEpochSecond)
          )
          val tokenWithoutSubject = JwtCirce.encode(noSubjectClaim, jwtKey, algorithm)
          authorization.getUserId(tokenWithoutSubject) should be(None)
        }
      }
      describe("return some userId") {
        it("when token is valid") {
          val claim = JwtClaim(
            expiration = Some(Instant.now.plusSeconds(120).getEpochSecond),
            issuedAt = Some(Instant.now.getEpochSecond),
            subject = Some(userId.toString)
          )
          val token = JwtCirce.encode(claim, jwtKey, algorithm)
          authorization.getUserId(token) should be(Some(userId))
        }
      }
    }
  }
}
