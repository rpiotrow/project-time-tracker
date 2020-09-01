package io.github.rpiotrow.ptt.read.web

import java.net.URLDecoder

import cats.implicits._
import eu.timepit.refined._
import eu.timepit.refined.auto._
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
import sttp.tapir.server.http4s._
import zio._
import zio.interop.catz._

object Routes {
  trait Service {
    def readSideRoutes(): HttpRoutes[Task]
  }

  def readSideRoutes(): RIO[Routes, HttpRoutes[Task]] = ZIO.access(_.get.readSideRoutes())

  val live: ZLayer[Services, Throwable, Routes] =
    ZLayer.fromServices[ProjectService.Service, StatisticsService.Service, Routes.Service](
      (projectService, statisticsService) => new RoutesLive(projectService, statisticsService)
    )
}

private class RoutesLive(
  private val projectService: ProjectService.Service,
  private val statisticsService: StatisticsService.Service
) extends Routes.Service {

  implicit private val serverOptions: Http4sServerOptions[Task] =
    Http4sServerOptions.default[Task].copy(decodeFailureHandler = DecodeFailure.decodeFailureHandler)

  override def readSideRoutes(): HttpRoutes[Task] = {
    projectListEndpoint.toRoutes { params =>
      projectList(params)
    } <+> projectDetailEndpoint.toRoutes { projectId =>
      projectDetail(projectId)
    } <+> statisticsEndpoint.toRoutes(params => {
      statistics(params)
    })
  }

  private def projectList(params: ProjectListParams): Task[Either[ApiError, List[ProjectOutput]]] = {
    projectService
      .list(params)
      .catchAll {
        case RepositoryThrowable(_) => ZIO.fail(ServerError("server.error"))
      }
      .either
  }

  private def projectDetail(projectId: ProjectId): Task[Either[ApiError, ProjectOutput]] = {
    (for {
      decodedInput <- decodeProjectId(projectId)
      task         <- projectDetailDecoded(decodedInput)
    } yield task).either
  }

  private def decodeProjectId(projectId: ProjectId) = {
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

  private def statistics(params: StatisticsParams): Task[Either[ApiError, StatisticsOutput]] = {
    statisticsService
      .read(params)
      .catchAll {
        case RepositoryThrowable(_) => ZIO.fail(ServerError("server.error"))
      }
      .either
  }
}
