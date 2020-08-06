package io.github.rpiotrow.ptt.gateway.web

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import io.github.rpiotrow.ptt.gateway.configuration.AppConfiguration

import scala.concurrent.ExecutionContextExecutor

object Gateway extends App {
  private val config = AppConfiguration.readOrThrow()

  implicit private val system: ActorSystem          = ActorSystem("Gateway")
  implicit private val ec: ExecutionContextExecutor = system.dispatcher

  private val readSideProxy  = new ServiceProxyLive(config.readSideService.host, config.readSideService.port)
  private val writeSideProxy = new ServiceProxyLive(config.writeSideService.host, config.writeSideService.port)

  private val routes = Routes.docsRoute ~ Routes.serviceRoute(readSideProxy, writeSideProxy)

  Http(system).bindAndHandle(handler = routes, interface = config.host, port = config.port)
}
