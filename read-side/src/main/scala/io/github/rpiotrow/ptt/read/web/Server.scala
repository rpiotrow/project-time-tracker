package io.github.rpiotrow.ptt.read.web

import fs2.Stream
import fs2.text.utf8
import io.github.rpiotrow.ptt.read.configuration.WebConfiguration
import org.http4s.headers.`Content-Type`
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.{Headers, MediaType, Response, Status}
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.interop.catz._

object Server {
  type ServerStream = Stream[RIO[WebEnv, *], Nothing]

  trait Service {
    def stream: RIO[Clock, ServerStream]
  }

  def stream: RIO[Server with WebEnv, ServerStream] = ZIO.accessM(_.get.stream)

  val live: ZLayer[Routes with Has[WebConfiguration], Throwable, Server] =
    ZLayer.fromServices[Routes.Service, WebConfiguration, Server.Service] { (routes, configuration) =>
      new Service() {
        override def stream: RIO[Clock, ServerStream] = {
          val jsonNotFound: Response[RIO[WebEnv, *]] =
            Response(
              Status.NotFound,
              body = Stream("""{"error": "Not found"}""").through(utf8.encode),
              headers = Headers(`Content-Type`(MediaType.application.json) :: Nil)
            )
          ZIO.runtime[Clock].flatMap { implicit runtime =>
            val httpApp            = routes.readSideRoutes().mapF(_.getOrElse(jsonNotFound))
            val httpAppWithLogging = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)
            ZIO.descriptor.map { d =>
              val server = BlazeServerBuilder[RIO[WebEnv, *]]
                .bindHttp(configuration.port, configuration.host)
                .withHttpApp(httpAppWithLogging)
                .serve
              server.drain
            }
          }
        }
      }
    }
}
