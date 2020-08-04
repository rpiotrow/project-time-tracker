package io.github.rpiotrow.projecttimetracker.web

import java.time.{Duration, LocalDateTime}
import java.util.UUID

import cats._
import cats.effect._
import cats.implicits._
import eu.timepit.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.string._
import fs2.Stream
import io.github.rpiotrow.projecttimetracker.api.error._
import io.github.rpiotrow.projecttimetracker.api.model.UserId
import io.github.rpiotrow.projecttimetracker.api.output._
import io.github.rpiotrow.projecttimetracker.api._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger.{httpApp => loggingHttpApp}
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.http4s._
import sttp.tapir.swagger.http4s.SwaggerHttp4s

object Main extends IOApp {
  def run(args: List[String]) = Server.stream[IO].compile.drain.as(ExitCode.Success)
}

object Server {

  def stream[F[_]: ContextShift: ConcurrentEffect: Parallel](implicit T: Timer[F]): Stream[F, Nothing] = {
    val docs = new SwaggerHttp4s(allEndpointsWithAuth.toOpenAPI("Project Time Tracker", "1.0").toYaml).routes
    BlazeServerBuilder[F](scala.concurrent.ExecutionContext.global)
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(loggingHttpApp(true, true)((mockedRoutes <+> docs).orNotFound))
      .serve
  }.drain

  private def mockedRoutes[F[_]: Sync: ContextShift](implicit M: Monad[F]) = {
    val projectList   = projectListEndpoint.toRoutes(_ => {
      val user1Id = randomUserId
      val user2Id = randomUserId
      val p1      = ProjectOutput("p1", LocalDateTime.now(), user1Id, Duration.ZERO, List())
      val p2      = ProjectOutput("p2", LocalDateTime.now(), user2Id, Duration.ZERO, List())
      List(p1, p2).asRight[ApiError].pure[F]
    })
    val projectDetail = projectDetailEndpoint.toRoutes(_ => {
      val ownerId = randomUserId
      val user1Id = randomUserId
      val user2Id = randomUserId
      val t1      = TaskOutput(user1Id, LocalDateTime.now(), Duration.ofHours(12), 8.some, "random comment".some)
      val t2      = TaskOutput(user2Id, LocalDateTime.now(), Duration.ofHours(2), None, None)
      val p       = ProjectOutput("p1", LocalDateTime.now(), ownerId, Duration.ofHours(14), List(t1, t2))
      p.asRight[ApiError]
        .pure[F]
    })
    val projectCreate = projectCreateEndpoint.toRoutes(_ => projectLocation[F])
    val projectUpdate = projectUpdateEndpoint.toRoutes(_ => projectLocation[F])
    val projectDelete = projectDeleteEndpoint.toRoutes(_ => ().asRight[ApiError].pure[F])
    val taskCreate    = taskCreateEndpoint.toRoutes(_ => taskLocation[F])
    val taskUpdate    = taskUpdateEndpoint.toRoutes(_ => taskLocation[F])
    val taskDelete    = taskDeleteEndpoint.toRoutes(_ => ().asRight[ApiError].pure[F])
    val statistics    = statisticsEndpoint.toRoutes(_ => {
      StatisticsOutput(
        numberOfTasks = 6,
        averageTaskDuration = Duration.ofMinutes(32),
        averageTaskVolume = 2.3,
        volumeWeightedAverageTaskDuration = Duration.ofMinutes(52)
      ).asRight[ApiError].pure[F]
    })
    projectList <+> projectDetail <+> projectCreate <+> projectUpdate <+> projectDelete <+>
      taskCreate <+> taskUpdate <+> taskDelete <+> statistics

  }

  private def randomUserId = UUID.randomUUID()

  private def taskLocation[F[_]: Monad] = {
    val location: LocationHeader = "http://localhost:8080/project/124/task/11"
    location.asRight[ApiError].pure[F]
  }

  private def projectLocation[F[_]: Monad] = {
    val location: LocationHeader = "http://localhost:8080/project/123"
    location.asRight[ApiError].pure[F]
  }

}
