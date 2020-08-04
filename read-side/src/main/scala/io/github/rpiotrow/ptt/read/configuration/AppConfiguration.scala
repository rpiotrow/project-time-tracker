package io.github.rpiotrow.ptt.read.configuration

case class AppConfiguration(databaseConfiguration: DatabaseConfiguration, webConfiguration: WebConfiguration)

case class DatabaseConfiguration(
  jdbcDriver: String,
  jdbcUrl: String,
  dbUsername: String,
  dbPassword: String,
  schema: String
)

case class WebConfiguration(host: String, port: Int)
