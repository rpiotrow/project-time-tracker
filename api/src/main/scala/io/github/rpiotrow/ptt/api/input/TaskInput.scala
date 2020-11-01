package io.github.rpiotrow.ptt.api.input

import java.time.{Duration, OffsetDateTime}

import cats.implicits._

case class TaskInput(startedAt: OffsetDateTime, duration: Duration, volume: Option[Int], comment: Option[String])

object TaskInput {
  private[api] val example = TaskInput(
    startedAt = OffsetDateTime.now(),
    duration = Duration.ofMinutes(30),
    volume = 10.some,
    comment = "Example comment for example task.".some
  )
}
