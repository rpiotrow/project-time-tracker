package io.github.rpiotrow.ptt.gateway.configuration

import pureconfig._
import pureconfig.generic.auto._

case class ServiceConfiguration(host: String, port: Int)

case class AppConfiguration(
  host: String,
  port: Int,
  readSideService: ServiceConfiguration,
  writeSideService: ServiceConfiguration
)

object AppConfiguration {
  def readOrThrow() = {
    ConfigSource.default.load[AppConfiguration] match {
      case Left(configReaderFailures) => {
        println(configReaderFailures.prettyPrint())
        throw new RuntimeException("config read error")
      }
      case Right(config)              => config
    }
  }

}
