package io.github.rpiotrow.ptt.write.repository

import java.time.{Clock, Instant}

import cats.implicits._
import io.getquill.{idiom => _}
import io.github.rpiotrow.ptt.api.model.{ProjectId, UserId}
import io.github.rpiotrow.ptt.write.entity.ProjectEntity

trait ProjectRepository {
  def create(projectId: ProjectId, owner: UserId): DBResult[ProjectEntity]
  def get(projectId: ProjectId): DBResult[Option[ProjectEntity]]
  def update(project: ProjectEntity, newProjectId: ProjectId): DBResult[ProjectEntity]
  def delete(project: ProjectEntity): DBResult[ProjectEntity]
}

object ProjectRepository {
  val live: ProjectRepository = new ProjectRepositoryLive(liveContext)
}

private[repository] class ProjectRepositoryLive(
  private val ctx: DBContext,
  private val clock: Clock = Clock.systemUTC()
) extends ProjectRepository {

  import ctx._

  private val projects = quote { querySchema[ProjectEntity]("ptt.projects") }

  override def create(projectId: ProjectId, owner: UserId): DBResult[ProjectEntity] = {
    val now    = Instant.now(clock)
    val entity = ProjectEntity(projectId = projectId, createdAt = now, deletedAt = None, owner = owner)
    run(quote { projects.insert(lift(entity)).returningGenerated(_.dbId) })
      .map(dbId => entity.copy(dbId = dbId))
  }

  override def get(projectId: ProjectId): DBResult[Option[ProjectEntity]] = {
    run(quote { projects.filter(_.projectId == lift(projectId)) }).map(_.headOption)
  }

  override def update(project: ProjectEntity, newProjectId: ProjectId): DBResult[ProjectEntity] = {
    run(quote {
      projects
        .filter(_.projectId == lift(project.projectId))
        .update(_.projectId -> lift(newProjectId))
    }).map({
      case 1 => project.copy(projectId = newProjectId)
      case _ => throw new RuntimeException(s"Project '${project.projectId}' not updated !!!")
    })
  }

  override def delete(project: ProjectEntity): DBResult[ProjectEntity] = {
    val now = Instant.now(clock)
    run(quote {
      projects
        .filter(e => e.projectId == lift(project.projectId) && e.deletedAt.isEmpty)
        .update(_.deletedAt -> lift(now.some))
    }).map({
      case 1 => project.copy(deletedAt = now.some)
      case _ => throw new RuntimeException(s"Project '${project.projectId}' not deleted !!!")
    })
  }

}
