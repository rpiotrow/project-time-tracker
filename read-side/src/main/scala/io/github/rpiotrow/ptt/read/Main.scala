package io.github.rpiotrow.ptt.read

import io.github.rpiotrow.ptt.read.configuration._
import io.github.rpiotrow.ptt.read.repository.{Repositories, postgreSQLRepositories}
import io.github.rpiotrow.ptt.read.service.liveServices
import io.github.rpiotrow.ptt.read.web.{Routes, Server}
import zio._
import zio.blocking.Blocking
import zio.config.syntax._
import zio.console.putStrLn
import zio.interop.catz._

object Main extends zio.App {

  type AppEnvironment = ZEnv with Server

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val configuration: Layer[Throwable, Has[AppConfiguration]]                    =
      Configuration.live
    val databaseConfiguration: ZLayer[Any, Throwable, Has[DatabaseConfiguration]] =
      configuration.narrow(_.databaseConfiguration)
    val webConfiguration: ZLayer[Any, Throwable, Has[WebConfiguration]]           =
      configuration.narrow(_.webConfiguration)

    val repositories: ZLayer[Any, Throwable, Repositories] =
      (databaseConfiguration ++ Blocking.live) >>> postgreSQLRepositories()
    val server                                             =
      (repositories >>> liveServices >>> Routes.live) ++ webConfiguration >>> Server.live

    val program: RIO[AppEnvironment, Unit] =
      for {
        serverStream <- Server.stream
        server       <- serverStream.compile.drain
      } yield server

    program
      .provideSomeLayer[ZEnv](server)
      .foldM(
        err => putStrLn(s"Execution failed with: $err").orDie *> IO.succeed(ExitCode.failure),
        _ => IO.succeed(ExitCode.success)
      )

  }
}
