package io.github.rpiotrow.ptt.e2e.factories

import java.time.Instant
import java.time.temporal.ChronoUnit

import eu.timepit.refined
import io.github.rpiotrow.ptt.api.input.BearerToken
import io.github.rpiotrow.ptt.api.model.UserId
import io.github.rpiotrow.ptt.e2e.configuration.End2EndTestsConfiguration.config
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

object AuthorizationTokenFactory {

  private val authorizationConfig = config.application.authorization

  private val key       = authorizationConfig.jwtSecret
  private val algorithm = JwtAlgorithm.fromString(authorizationConfig.jwtAlgorithm) match {
    case algorithm: JwtHmacAlgorithm => algorithm
    case _                           => throw new RuntimeException("unsupported JWT algorithm")
  }

  def generateValidToken(userId: UserId): BearerToken = {
    val now   = Instant.now
    val claim = JwtClaim(
      subject = Some(userId.id.toString),
      expiration = Some(now.plus(15, ChronoUnit.MINUTES).getEpochSecond),
      issuedAt = Some(now.getEpochSecond)
    )
    val jwt   = JwtCirce.encode(claim, key, algorithm)

    val either: Either[String, BearerToken] = refined.refineV(jwt)
    either.fold(err => throw new IllegalArgumentException(err), identity)
  }
}
