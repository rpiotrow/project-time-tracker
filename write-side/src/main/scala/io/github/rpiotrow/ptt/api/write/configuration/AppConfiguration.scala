package io.github.rpiotrow.ptt.api.write.configuration

case class AppConfiguration(
  databaseConfiguration: DatabaseConfiguration,
  webConfiguration: WebConfiguration,
  gatewayConfiguration: GatewayConfiguration
)

case class DatabaseConfiguration(jdbcDriver: String, jdbcUrl: String, dbUsername: String, dbPassword: String)

case class WebConfiguration(host: String, port: Int)

case class GatewayConfiguration(address: String)
