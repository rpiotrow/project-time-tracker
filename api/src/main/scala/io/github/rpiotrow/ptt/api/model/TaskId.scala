package io.github.rpiotrow.ptt.api.model

import java.util.UUID

case class TaskId(id: UUID) extends AnyVal

object TaskId {
  def apply(value: String): TaskId = TaskId(UUID.fromString(value))
  def random(): TaskId             = TaskId(UUID.randomUUID())
  private[api] val example: TaskId = TaskId("71a0253a-4c8c-4f74-9d75-c745b004c703")
}
