package io.github.rpiotrow.ptt.api.input

import java.time.{Duration, LocalDateTime}

import cats.implicits._

case class TaskInput(startedAt: LocalDateTime, duration: Duration, volume: Option[Int], comment: Option[String])

object TaskInput {
  private[api] val example = TaskInput(
    startedAt = LocalDateTime.now(),
    duration = Duration.ofMinutes(30),
    volume = 10.some,
    comment = "Example comment for example task.".some
  )
}
