package io.github.rpiotrow.ptt.read.web

import java.net.URLDecoder
import cats.implicits._
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection._
import io.github.rpiotrow.ptt.api.model._
import io.github.rpiotrow.ptt.api.ProjectEndpoints._
import io.github.rpiotrow.ptt.api.StatisticsEndpoints._
import io.github.rpiotrow.ptt.api.error._
import io.github.rpiotrow.ptt.api.output._
import io.github.rpiotrow.ptt.api.param._
import io.github.rpiotrow.ptt.read.repository._
import io.github.rpiotrow.ptt.read.service._
import org.http4s.HttpRoutes
import org.slf4j.LoggerFactory
import sttp.tapir.server.http4s.Http4sServerOptions.Log
import sttp.tapir.server.http4s._
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.server.interceptor.exception.DefaultExceptionHandler
import sttp.tapir.server.interceptor.reject.RejectInterceptor
import zio._
import zio.interop.catz._

object Routes {
  type HttpRoutesZIO = HttpRoutes[RIO[WebEnv, *]]

  trait Service {
    def readSideRoutes(): HttpRoutesZIO
  }

  def readSideRoutes(): RIO[Routes, HttpRoutesZIO] = ZIO.access(_.get.readSideRoutes())

  val live: ZLayer[Services, Throwable, Routes] =
    ZLayer.fromServices[ProjectService.Service, StatisticsService.Service, Routes.Service](
      (projectService, statisticsService) => new RoutesLive(projectService, statisticsService)
    )
}

private class RoutesLive(
  private val projectService: ProjectService.Service,
  private val statisticsService: StatisticsService.Service
) extends Routes.Service {

  private val serverOptions: Http4sServerOptions[RIO[WebEnv, *], RIO[WebEnv, *]] =
    Http4sServerOptions.customInterceptors(
      Some(RejectInterceptor.default), Some(DefaultExceptionHandler), Some(Log.defaultServerLog),
      decodeFailureHandler = DecodeFailure.decodeFailureHandler
    )

  private val logger = LoggerFactory.getLogger("RoutesLive")

  override def readSideRoutes(): Routes.HttpRoutesZIO = {
    val interpreter = ZHttp4sServerInterpreter(serverOptions)

    interpreter.from(projectListEndpoint) { params =>
      projectList(params)
    }.toRoutes <+> interpreter.from(projectDetailEndpoint) { projectId =>
      projectDetail(projectId)
    }.toRoutes <+> interpreter.from(statisticsEndpoint) { params =>
      statistics(params)
    }.toRoutes
  }

  private def projectList(params: ProjectListParams): IO[ApiError, List[ProjectOutput]] = {
    projectService
      .list(params)
      .catchAll {
        case RepositoryThrowable(e) => {
          logger.error(e.getMessage, e)
          ZIO.fail(ServerError("server.error"))
        }
      }
  }

  private def projectDetail(projectId: ProjectId): IO[ApiError, ProjectOutput] =
    for {
      decodedInput <- decodeProjectId(projectId)
      task         <- projectDetailDecoded(decodedInput)
    } yield task

  private def decodeProjectId(projectId: ProjectId): IO[InvalidInput, ProjectId] = {
    ZIO
      .fromEither(refineV[NonEmpty](URLDecoder.decode(projectId.value, "UTF-8")))
      .orElseFail(InvalidInput("empty project id"))
  }

  private def projectDetailDecoded(decodedInput: ProjectId): IO[ApiError, ProjectOutput] = {
    projectService
      .one(decodedInput)
      .catchAll {
        case EntityNotFound(id)     => ZIO.fail(NotFound(s"project '$id' not found"))
        case RepositoryThrowable(_) => ZIO.fail(ServerError("server.error"))
      }
  }

  private def statistics(params: StatisticsParams): IO[ApiError, StatisticsOutput] = {
    statisticsService
      .read(params)
      .catchAll {
        case RepositoryThrowable(_) => ZIO.fail(ServerError("server.error"))
      }
  }
}
