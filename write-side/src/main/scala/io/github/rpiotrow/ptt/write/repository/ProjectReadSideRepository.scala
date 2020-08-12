package io.github.rpiotrow.ptt.write.repository

import io.getquill.{idiom => _}
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity}

trait ProjectReadSideRepository {
  def newProject(project: ProjectEntity): DBResult[ProjectReadSideEntity]
}

object ProjectReadSideRepository {
  lazy val live: ProjectReadSideRepository = new ProjectReadSideRepositoryLive(liveContext)
}

private[repository] class ProjectReadSideRepositoryLive(private val ctx: DBContext) extends ProjectReadSideRepository {

  import ctx._

  private val projectsReadSide = quote { querySchema[ProjectReadSideEntity]("ptt_read_model.projects") }

  override def newProject(project: ProjectEntity): DBResult[ProjectReadSideEntity] = {
    val entity = ProjectReadSideEntity(project)
    run(quote { projectsReadSide.insert(lift(entity)).returningGenerated(_.dbId) })
      .map(dbId => entity.copy(dbId = dbId))
  }

}
