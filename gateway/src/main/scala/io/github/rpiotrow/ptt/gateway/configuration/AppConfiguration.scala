package io.github.rpiotrow.ptt.gateway.configuration

import pureconfig._
import pureconfig.generic.auto._

case class ServiceConfiguration(host: String, port: Int)

case class AuthorizationConfig(jwtSecret: String, jwtAlgorithm: String)

case class AppConfiguration(
  host: String,
  port: Int,
  authorization: AuthorizationConfig,
  readSideService: ServiceConfiguration,
  writeSideService: ServiceConfiguration
)

object AppConfiguration {
  def readOrThrow(): AppConfiguration = {
    ConfigSource.default.load[AppConfiguration] match {
      case Left(configReaderFailures) => {
        println(configReaderFailures.prettyPrint())
        throw new RuntimeException("config read error")
      }
      case Right(config)              => config
    }
  }
}
