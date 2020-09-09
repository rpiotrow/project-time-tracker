package io.github.rpiotrow.ptt.e2e.configuration

import pureconfig._
import pureconfig.generic.auto._

case class ApplicationConfig(baseUri: String, authorization: AuthorizationConfig)

case class AuthorizationConfig(jwtSecret: String, jwtAlgorithm: String)

case class End2EndTestsConfiguration(application: ApplicationConfig)

object End2EndTestsConfiguration {

  lazy val config: End2EndTestsConfiguration = readOrThrow()

  private def readOrThrow(): End2EndTestsConfiguration = {
    ConfigSource.default.load[End2EndTestsConfiguration] match {
      case Left(configReaderFailures) => {
        println(configReaderFailures.prettyPrint())
        throw new RuntimeException("config read error")
      }
      case Right(config)              => config
    }
  }
}
