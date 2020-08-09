package io.github.rpiotrow.ptt.api.write.configuration

case class AppConfiguration(webConfiguration: WebConfiguration, gatewayAddress: GatewayConfiguration)

case class WebConfiguration(host: String, port: Int)

case class GatewayConfiguration(address: String)
