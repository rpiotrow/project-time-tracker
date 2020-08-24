package io.github.rpiotrow.ptt.gateway.web

import java.util.UUID

import io.github.rpiotrow.ptt.gateway.configuration.AuthorizationConfig
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtCirce}

import scala.util.Try

trait Authorization {
  def getUserId(rawJwtToken: String): Option[UUID]
}

class AuthorizationLive(private val config: AuthorizationConfig) extends Authorization {

  private val jwtSecret = config.jwtSecret
  private val jwtAlgorithm = {
    JwtAlgorithm.fromString(config.jwtAlgorithm) match {
      case algorithm: JwtHmacAlgorithm => algorithm
      case _                           => throw new RuntimeException("unsupported JWT algorithm")
    }
  }

  def getUserId(rawJwtToken: String): Option[UUID] = {
    for {
      jwtToken <- JwtCirce.decode(rawJwtToken, jwtSecret, Seq(jwtAlgorithm)).toOption
      subject  <- jwtToken.subject
      userId   <- Try(UUID.fromString(subject)).toOption
    } yield userId
  }

}
