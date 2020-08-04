package io.github.rpiotrow.ptt.read.web

import fs2.Stream
import io.github.rpiotrow.ptt.read.configuration.WebConfiguration
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import zio.clock.Clock
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio._
import zio.config.Config

object Server {
  trait Service {
    def stream: RIO[Clock, Stream[Task, Nothing]]
  }

  def stream: RIO[Server with Clock, Stream[Task, Nothing]] = ZIO.accessM(_.get.stream)

  val live: ZLayer[Routes with Config[WebConfiguration], Throwable, Server] =
    ZLayer.fromServices[Routes.Service, WebConfiguration, Server.Service] { (routes, configuration) =>
      new Service() {
        override def stream: RIO[Clock, Stream[Task, Nothing]] =
          ZIO.runtime[Clock].flatMap { implicit runtime =>
            val httpApp            = routes.readSideRoutes().orNotFound
            val httpAppWithLogging = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)
            ZIO.descriptor.map { d =>
              val server = BlazeServerBuilder[Task](d.executor.asEC)
                .bindHttp(configuration.port, configuration.host)
                .bindHttp(8081, "0.0.0.0")
                .withHttpApp(httpAppWithLogging)
                .serve
              server.drain
            }
          }
      }
    }
}
