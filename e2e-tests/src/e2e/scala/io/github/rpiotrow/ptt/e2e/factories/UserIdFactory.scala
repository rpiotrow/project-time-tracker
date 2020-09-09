package io.github.rpiotrow.ptt.e2e.factories

import java.util.UUID

import io.github.rpiotrow.ptt.api.model.UserId

object UserIdFactory {
  def generateUserId(): UserId = UserId(UUID.randomUUID())
}
