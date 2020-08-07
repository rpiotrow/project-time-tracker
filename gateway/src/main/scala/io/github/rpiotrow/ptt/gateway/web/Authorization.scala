package io.github.rpiotrow.ptt.gateway.web

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{complete, headerValueByName, provide}
import io.github.rpiotrow.ptt.api.model.UserId
import io.github.rpiotrow.ptt.gateway.configuration.AuthorizationConfig
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtCirce}
import sttp.model.HeaderNames

import scala.util.Try

trait Authorization {
  def check: Directive1[UserId]
}

class AuthorizationLive(private val config: AuthorizationConfig) extends Authorization {

  override def check: Directive1[UserId] = {
    headerValueByName(HeaderNames.Authorization).flatMap { authorizationHeader =>
      userId(authorizationHeader) match {
        case Some(userId) => provide(userId)
        case _            => complete(StatusCodes.Unauthorized)
      }
    }
  }

  private def getJwtRawToken(authorizationToken: String): Option[String] = {
    val authorizationTokenSplit = authorizationToken.split(" ")
    if (authorizationTokenSplit.length == 2)
      Some(authorizationTokenSplit(1))
    else
      None
  }

  private val jwtSecret = config.jwtSecret
  private val jwtAlgorithm = {
    JwtAlgorithm.fromString(config.jwtAlgorithm) match {
      case algorithm: JwtHmacAlgorithm => algorithm
      case _                           => throw new RuntimeException("unsupported JWT algorithm")
    }
  }

  private def userId(authorizationToken: String): Option[UUID] = {
    for {
      rawJwtToken <- getJwtRawToken(authorizationToken)
      jwtToken    <- JwtCirce.decode(rawJwtToken, jwtSecret, Seq(jwtAlgorithm)).toOption
      subject     <- jwtToken.subject
      userId      <- Try(UUID.fromString(subject)).toOption
    } yield userId
  }

}
