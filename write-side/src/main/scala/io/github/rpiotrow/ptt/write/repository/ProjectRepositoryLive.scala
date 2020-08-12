package io.github.rpiotrow.ptt.write.repository

import java.time.{Clock, LocalDateTime}
import java.util.UUID

import doobie.ConnectionIO
import io.getquill.{idiom => _}
import io.github.rpiotrow.ptt.write.entity.ProjectEntity

trait ProjectRepository {
  def create(projectId: String, owner: UUID): ConnectionIO[ProjectEntity]
  def get(projectId: String): ConnectionIO[Option[ProjectEntity]]
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

  override def create(projectId: String, owner: UUID): ConnectionIO[ProjectEntity] = {
    val now    = LocalDateTime.now(clock)
    val entity = ProjectEntity(projectId = projectId, createdAt = now, updatedAt = now, deletedAt = None, owner = owner)
    run(quote { projects.insert(lift(entity)).returningGenerated(_.dbId) })
      .map(dbId => entity.copy(dbId = dbId))
  }

  override def get(projectId: String): ConnectionIO[Option[ProjectEntity]] = {
    run(quote { projects.filter(_.projectId == lift(projectId)) }).map(_.headOption)
  }

}
