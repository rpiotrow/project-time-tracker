package io.github.rpiotrow.ptt.write

import cats.effect.{ExitCode, IO, IOApp}
import io.github.rpiotrow.ptt.write.web.Server

object WriteSide extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    for {
      serverStream <- Server.stream[IO]
      exit         <- serverStream.compile.drain.as(ExitCode.Success)
    } yield exit

}
