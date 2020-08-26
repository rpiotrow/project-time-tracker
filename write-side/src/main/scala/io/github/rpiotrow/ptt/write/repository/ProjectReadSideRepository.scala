package io.github.rpiotrow.ptt.write.repository

import java.time.{Duration, LocalDateTime}

import cats.implicits._
import io.getquill.{idiom => _}
import io.github.rpiotrow.ptt.write.entity.{ProjectEntity, ProjectReadSideEntity, TaskEntity, TaskReadSideEntity}

trait ProjectReadSideRepository {
  def get(projectId: String): DBResult[Option[ProjectReadSideEntity]]

  def newProject(project: ProjectEntity): DBResult[ProjectReadSideEntity]
  def updateProject(projectId: String, updated: ProjectEntity): DBResult[Unit]
  def deleteProject(dbId: Long, projectId: String, deletedAt: LocalDateTime): DBResult[Unit]

  def addDuration(projectDbId: Long, duration: Duration): DBResult[Unit]
  def subtractDuration(projectDbId: Long, duration: Duration): DBResult[Unit]
}

object ProjectReadSideRepository {
  lazy val live: ProjectReadSideRepository = new ProjectReadSideRepositoryLive(liveContext)
}

private[repository] class ProjectReadSideRepositoryLive(private val ctx: DBContext)
    extends ProjectReadSideRepository
    with ReadSideRepositoryBase {

  import ctx._

  private val projectsReadSide = quote { querySchema[ProjectReadSideEntity]("ptt_read_model.projects") }

  override def get(projectId: String): DBResult[Option[ProjectReadSideEntity]] =
    run(quote {
      projectsReadSide
        .filter(_.projectId == lift(projectId))
    }).map(_.headOption)

  override def newProject(project: ProjectEntity): DBResult[ProjectReadSideEntity] = {
    val entity = ProjectReadSideEntity(project)
    run(quote { projectsReadSide.insert(lift(entity)).returningGenerated(_.dbId) })
      .map(dbId => entity.copy(dbId = dbId))
  }

  override def updateProject(projectId: String, updated: ProjectEntity): DBResult[Unit] = {
    run(quote {
      projectsReadSide
        .filter(_.projectId == lift(projectId))
        .update(_.projectId -> lift(updated.projectId), _.updatedAt -> lift(updated.updatedAt))
    }).map(logIfNotUpdated(s"no project '${projectId}' in read model"))
  }

  override def deleteProject(dbId: Long, projectId: String, deletedAt: LocalDateTime): DBResult[Unit] = {
    run(quote {
      projectsReadSide
        .filter(p => p.dbId == lift(dbId) && p.deletedAt.isEmpty)
        .update(
          _.deletedAt   -> lift(deletedAt.some),
          _.updatedAt   -> lift(deletedAt),
          _.durationSum -> lift(Duration.ZERO)
        )
    }).map(logIfNotUpdated(s"no project '$projectId' in read model"))
  }

  override def addDuration(projectDbId: Long, duration: Duration): DBResult[Unit] =
    run(quote {
      projectsReadSide
        .filter(_.dbId == lift(projectDbId))
        .update(e => e.durationSum -> (e.durationSum + lift(duration)))
    }).map(logIfNotUpdated(s"no project with dbId ${projectDbId} in read model"))

  override def subtractDuration(projectDbId: Long, duration: Duration): DBResult[Unit] =
    run(quote {
      projectsReadSide
        .filter(_.dbId == lift(projectDbId))
        .update(e => e.durationSum -> (e.durationSum - lift(duration)))
    }).map(logIfNotUpdated(s"no project with dbId ${projectDbId} in read model"))

}
