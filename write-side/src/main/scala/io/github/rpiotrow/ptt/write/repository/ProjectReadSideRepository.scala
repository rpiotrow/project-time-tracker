package io.github.rpiotrow.ptt.write.repository

import io.getquill.{idiom => _}
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity, TaskEntity}

trait ProjectReadSideRepository {
  def newProject(project: ProjectEntity): DBResult[ProjectReadSideEntity]
  def deleteProject(project: ProjectEntity): DBResult[Unit]

  def addToProjectDuration(taskReadModel: TaskEntity): DBResult[Unit]
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

  override def deleteProject(project: ProjectEntity): DBResult[Unit] = {
    run(quote {
      projectsReadSide
        .filter(_.projectId == lift(project.projectId))
        .update(_.deletedAt -> lift(project.deletedAt), _.updatedAt -> lift(project.updatedAt))
    }).map(_ => ())
  }

  override def addToProjectDuration(taskReadModel: TaskEntity): DBResult[Unit] = {
    run(quote {
      projectsReadSide
        .filter(_.dbId == lift(taskReadModel.projectDbId))
        .update(e => e.durationSum -> (e.durationSum + lift(taskReadModel.duration)))
    }).map(_ => ())
  }

}
