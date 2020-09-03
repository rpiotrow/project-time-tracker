package io.github.rpiotrow.ptt.api.model

import java.util.UUID

case class UserId(id: UUID) extends AnyVal

object UserId {
  def apply(value: String): UserId = UserId(UUID.fromString(value))
  private[api] val example: UserId = UserId("c5026130-043c-46be-9966-3299acf924e2")
}
