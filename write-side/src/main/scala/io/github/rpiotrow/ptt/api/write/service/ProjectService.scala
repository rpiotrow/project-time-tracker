package io.github.rpiotrow.ptt.api.write.service

import io.github.rpiotrow.ptt.api.error.ApiError
import io.github.rpiotrow.ptt.api.input.ProjectInput
import io.github.rpiotrow.ptt.api.output.ProjectOutput
import zio.IO

object ProjectService {
  trait Service {
    def create(input: ProjectInput): IO[ApiError, ProjectOutput]
  }

  def live(): Service = new ProjectServiceLive()
}

private class ProjectServiceLive extends ProjectService.Service {
  override def create(input: ProjectInput): IO[ApiError, ProjectOutput] = ???
}
