package io.github.rpiotrow.ptt.read.configuration

case class DatabaseConfiguration(
  jdbcDriver: String,
  jdbcUrl: String,
  dbUsername: String,
  dbPassword: String,
  schema: String
)
