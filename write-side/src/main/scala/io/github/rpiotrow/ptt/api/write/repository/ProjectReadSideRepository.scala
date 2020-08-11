package io.github.rpiotrow.ptt.api.write.repository

import doobie.ConnectionIO
import io.getquill.{idiom => _}
import io.github.rpiotrow.ptt.api.write.entity.{ProjectEntity, ProjectReadSideEntity}

object ProjectReadSideRepository {
  trait Service {
    def newProject(project: ProjectEntity): ConnectionIO[ProjectReadSideEntity]
  }
  def live(ctx: DBContext): Service = new ProjectReadSideRepositoryLive(ctx)
}

private class ProjectReadSideRepositoryLive(private val ctx: DBContext) extends ProjectReadSideRepository.Service {

  import ctx._

  private val projectsReadSide = quote { querySchema[ProjectReadSideEntity]("ptt_read_model.projects") }

  def newProject(project: ProjectEntity): ConnectionIO[ProjectReadSideEntity] = {
    val entity = ProjectReadSideEntity(project)
    run(quote { projectsReadSide.insert(lift(entity)).returningGenerated(_.dbId) })
      .map(dbId => entity.copy(dbId = dbId))
  }

}
