package io.github.rpiotrow.ptt.write.web

import java.util.concurrent.Executors

import cats.Monad
import cats.effect._
import fs2.Stream
import io.github.rpiotrow.ptt.write.configuration.AppConfiguration
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Server {

  def stream[F[_]: Monad: ContextShift: ConcurrentEffect: Timer](): Resource[F, Stream[F, Nothing]] = {
    val webConfiguration = AppConfiguration.live.webConfiguration
    Routes
      .live[F]()
      .map(routes => {
        val httpApp        = routes.writeSideRoutes().orNotFound
        val loggingHttpApp = org.http4s.server.middleware.Logger.httpApp(true, true)(httpApp)

        val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(webConfiguration.threadPoolSize))

        val serverStream = BlazeServerBuilder[F](ec)
          .bindHttp(webConfiguration.port, webConfiguration.host)
          .withHttpApp(loggingHttpApp)
          .serve

        serverStream.drain
      })
  }

}
