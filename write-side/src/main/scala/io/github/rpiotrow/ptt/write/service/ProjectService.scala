package io.github.rpiotrow.ptt.write.service

import cats.data.EitherT
import cats.effect.IO
import io.github.rpiotrow.ptt.api.input.ProjectInput
import io.github.rpiotrow.ptt.api.model.UserId
import io.github.rpiotrow.ptt.api.output.ProjectOutput

trait ProjectService {
  def create(input: ProjectInput, owner: UserId): EitherT[IO, AppFailure, ProjectOutput]
}

object ProjectService {
  val live: IO[ProjectService] = IO { new ProjectServiceLive() }
}

private class ProjectServiceLive extends ProjectService {
  override def create(input: ProjectInput, owner: UserId): EitherT[IO, AppFailure, ProjectOutput] = ???
}
