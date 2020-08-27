package io.github.rpiotrow.ptt.write.web

import java.util.concurrent.Executors

import cats.Monad
import cats.effect._
import fs2.Stream
import fs2.text.utf8Encode
import io.github.rpiotrow.ptt.write.configuration.AppConfiguration
import org.http4s.headers.`Content-Type`
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{Headers, MediaType, Response, Status}

import scala.concurrent.ExecutionContext

object Server {

  def stream[F[_]: Monad: ContextShift: ConcurrentEffect: Timer](): Resource[F, Stream[F, Nothing]] = {
    val webConfiguration          = AppConfiguration.live.webConfiguration
    val jsonNotFound: Response[F] =
      Response(
        Status.NotFound,
        body = Stream("""{"error": "Not found"}""").through(utf8Encode),
        headers = Headers(`Content-Type`(MediaType.application.json) :: Nil)
      )
    Routes
      .live[F]()
      .map(routes => {
        val httpApp        = routes.writeSideRoutes().mapF(_.getOrElse(jsonNotFound))
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
