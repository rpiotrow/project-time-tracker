package io.github.rpiotrow.ptt.write.web

import cats.implicits._
import cats.Monad
import cats.effect._
import fs2.Stream
import io.github.rpiotrow.ptt.write.configuration.AppConfiguration
import io.github.rpiotrow.ptt.write.configuration.AppConfiguration
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Server {

  def stream[F[_]: Monad: ContextShift: ConcurrentEffect: Timer](): F[Stream[F, Nothing]] = {
    val webConfiguration = AppConfiguration.live.webConfiguration
    for {
      routes <- Routes.live[F]()

      httpApp        = routes.writeSideRoutes().orNotFound
      loggingHttpApp = org.http4s.server.middleware.Logger.httpApp(true, true)(httpApp)

      serverStream = BlazeServerBuilder[F](ExecutionContext.global)
        .bindHttp(webConfiguration.port, webConfiguration.host)
        .withHttpApp(loggingHttpApp)
        .serve
    } yield serverStream.drain
  }

}
