package io.github.rpiotrow.ptt.write.configuration

import org.slf4j.LoggerFactory
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource, KebabCase}
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._

case class AppConfiguration(
  databaseConfiguration: DatabaseConfiguration,
  webConfiguration: WebConfiguration,
  gatewayConfiguration: GatewayConfiguration
)

case class DatabaseConfiguration(jdbcDriver: String, jdbcUrl: String, dbUsername: String, dbPassword: String)

case class WebConfiguration(host: String, port: Int)

case class GatewayConfiguration(address: String)

object AppConfiguration {

  lazy val live: AppConfiguration = {

    implicit def hint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))

    ConfigSource.default.load[AppConfiguration] match {
      case Left(e)       =>
        val message = s"Configuration errors: ${e.prettyPrint()}"
        LoggerFactory
          .getLogger(AppConfiguration.getClass)
          .warn(message)
        throw new RuntimeException(message)
      case Right(config) => config
    }
  }

}
