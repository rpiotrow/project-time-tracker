package io.github.rpiotrow.ptt.write.web

import cats.effect._
import fs2.Stream
import io.github.rpiotrow.ptt.write.configuration.AppConfiguration
import io.github.rpiotrow.ptt.write.configuration.AppConfiguration
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Server {

  def stream(implicit CS: ContextShift[IO], C: Concurrent[IO], T: Timer[IO]): IO[Stream[IO, Nothing]] = {
    for {
      config <- AppConfiguration.live
      webConfiguration = config.webConfiguration
      routes <- Routes.live

      httpApp        = routes.writeSideRoutes().orNotFound
      loggingHttpApp = org.http4s.server.middleware.Logger.httpApp(true, true)(httpApp)

      serverStream = BlazeServerBuilder[IO](ExecutionContext.global)
        .bindHttp(webConfiguration.port, webConfiguration.host)
        .withHttpApp(loggingHttpApp)
        .serve
    } yield serverStream.drain
  }

}
