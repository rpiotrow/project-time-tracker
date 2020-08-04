package io.github.rpiotrow.ptt.api.input

import java.time.{Duration, LocalDateTime}

case class TaskInput(startedAt: LocalDateTime, duration: Duration, volume: Option[Int], comment: Option[String])
