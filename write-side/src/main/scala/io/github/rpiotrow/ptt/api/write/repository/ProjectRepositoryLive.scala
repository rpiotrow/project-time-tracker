package io.github.rpiotrow.ptt.api.write.repository

import java.time.LocalDateTime
import java.util.UUID

import doobie.ConnectionIO
import doobie.quill.DoobieContext
import io.getquill.{SnakeCase, idiom => _}
import io.github.rpiotrow.ptt.api.model.UserId
import io.github.rpiotrow.ptt.api.write.entity.ProjectEntity

object ProjectRepository {
  trait Service {
    def create(projectId: String, owner: UUID): ConnectionIO[ProjectEntity]
    def get(projectId: String): ConnectionIO[Option[ProjectEntity]]
  }
  def live(ctx: DBContext): Service = new ProjectRepositoryLive(ctx)
}

private class ProjectRepositoryLive(private val ctx: DBContext) extends ProjectRepository.Service {

  import ctx._

  private val projects = quote { querySchema[ProjectEntity]("ptt.projects") }

  override def create(projectId: String, owner: UUID): ConnectionIO[ProjectEntity] = {
    val now    = LocalDateTime.now()
    val entity = ProjectEntity(projectId = projectId, createdAt = now, updatedAt = now, deletedAt = None, owner = owner)
    run(quote { projects.insert(lift(entity)).returningGenerated(_.dbId) })
      .map(dbId => entity.copy(dbId = dbId))
  }

  override def get(projectId: String): ConnectionIO[Option[ProjectEntity]] = {
    run(quote { projects.filter(_.projectId == lift(projectId)) }).map(_.headOption)
  }

}
