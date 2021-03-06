package io.github.rpiotrow.ptt.read.web

import fs2.Stream
import fs2.text.utf8Encode
import io.github.rpiotrow.ptt.read.configuration.WebConfiguration
import org.http4s.headers.`Content-Type`
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.{Headers, MediaType, Response, Status}
import zio._
import zio.clock.Clock
import zio.config._
import zio.interop.catz._
import zio.interop.catz.implicits._

object Server {
  trait Service {
    def stream: RIO[Clock, Stream[Task, Nothing]]
  }

  def stream: RIO[Server with Clock, Stream[Task, Nothing]] = ZIO.accessM(_.get.stream)

  val live: ZLayer[Routes with ZConfig[WebConfiguration], Throwable, Server] =
    ZLayer.fromServices[Routes.Service, WebConfiguration, Server.Service] { (routes, configuration) =>
      new Service() {
        override def stream: RIO[Clock, Stream[Task, Nothing]] = {
          val jsonNotFound: Response[Task] =
            Response(
              Status.NotFound,
              body = Stream("""{"error": "Not found"}""").through(utf8Encode),
              headers = Headers(`Content-Type`(MediaType.application.json) :: Nil)
            )
          ZIO.runtime[Clock].flatMap { implicit runtime =>
            val httpApp            = routes.readSideRoutes().mapF(_.getOrElse(jsonNotFound))
            val httpAppWithLogging = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)
            ZIO.descriptor.map { d =>
              val server = BlazeServerBuilder[Task](d.executor.asEC)
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
