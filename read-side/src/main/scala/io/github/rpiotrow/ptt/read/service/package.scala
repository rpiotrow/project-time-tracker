package io.github.rpiotrow.ptt.read

import io.github.rpiotrow.ptt.read.repository._
import zio.{Has, ZLayer}

package object service {

  type ProjectService    = Has[ProjectService.Service]
  type StatisticsService = Has[StatisticsService.Service]

  type Services = ProjectService with StatisticsService

  val liveServices: ZLayer[Repositories, Throwable, Services] =
    ZLayer.fromServicesMany[TaskRepository.Service, ProjectRepository.Service, StatisticsRepository.Service, Services](
      (taskRepository, projectRepository, statisticsRepository) => {
        val projectService    = ProjectService.live(projectRepository, taskRepository)
        val statisticsService = StatisticsService.live(statisticsRepository)
        Has(projectService) ++ Has(statisticsService)
      }
    )
}
