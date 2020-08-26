package io.github.rpiotrow.ptt.write.repository

import java.time.{Clock, LocalDateTime}
import java.util.UUID

import cats.implicits._
import io.getquill.{idiom => _}
import io.github.rpiotrow.ptt.write.entity.ProjectEntity

trait ProjectRepository {
  def create(projectId: String, owner: UUID): DBResult[ProjectEntity]
  def get(projectId: String): DBResult[Option[ProjectEntity]]
  def update(project: ProjectEntity, newProjectId: String): DBResult[ProjectEntity]
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

  override def create(projectId: String, owner: UUID): DBResult[ProjectEntity] = {
    val now    = LocalDateTime.now(clock)
    val entity = ProjectEntity(projectId = projectId, createdAt = now, updatedAt = now, deletedAt = None, owner = owner)
    run(quote { projects.insert(lift(entity)).returningGenerated(_.dbId) })
      .map(dbId => entity.copy(dbId = dbId))
  }

  override def get(projectId: String): DBResult[Option[ProjectEntity]] = {
    run(quote { projects.filter(_.projectId == lift(projectId)) }).map(_.headOption)
  }

  override def update(project: ProjectEntity, newProjectId: String): DBResult[ProjectEntity] = {
    val now = LocalDateTime.now(clock)
    run(quote {
      projects
        .filter(_.projectId == lift(project.projectId))
        .update(_.projectId -> lift(newProjectId))
    }).map({
      case 1 => project.copy(projectId = newProjectId, updatedAt = now)
      case _ => throw new RuntimeException(s"Project '${project.projectId}' not updated !!!")
    })
  }

  override def delete(project: ProjectEntity): DBResult[ProjectEntity] = {
    val now = LocalDateTime.now(clock)
    run(quote {
      projects
        .filter(e => e.projectId == lift(project.projectId) && e.deletedAt.isEmpty)
        .update(_.deletedAt -> lift(now.some), _.updatedAt -> lift(now))
    }).map({
      case 1 => project.copy(deletedAt = now.some, updatedAt = now)
      case _ => throw new RuntimeException(s"Project '${project.projectId}' not deleted !!!")
    })
  }

}
