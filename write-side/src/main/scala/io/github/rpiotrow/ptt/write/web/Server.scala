package io.github.rpiotrow.ptt.write.web

import cats.effect._
import fs2.Stream
import fs2.text.utf8
import io.github.rpiotrow.ptt.write.configuration.AppConfiguration
import org.http4s.headers.`Content-Type`
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.{Headers, MediaType, Response, Status}

object Server {

  def stream[F[_]: Async](): Resource[F, Stream[F, Nothing]] = {
    val webConfiguration          = AppConfiguration.live.webConfiguration
    val jsonNotFound: Response[F] =
      Response(
        Status.NotFound,
        body = Stream("""{"error": "Not found"}""").through(utf8.encode),
        headers = Headers(`Content-Type`(MediaType.application.json) :: Nil)
      )
    Routes
      .live[F]()
      .map(routes => {
        val httpApp        = routes.writeSideRoutes().mapF(_.getOrElse(jsonNotFound))
        val loggingHttpApp = org.http4s.server.middleware.Logger.httpApp(true, true)(httpApp)

        val serverStream = BlazeServerBuilder[F]
          .bindHttp(webConfiguration.port, webConfiguration.host)
          .withHttpApp(loggingHttpApp)
          .serve

        serverStream.drain
      })
  }

}
