package io.github.rpiotrow.ptt.e2e.factories

import java.time.{Duration, LocalDateTime}

import io.github.rpiotrow.ptt.api.input.TaskInput

import scala.util.Random

object TaskFactory {
  def generateTaskInput(): TaskInput = {
    new TaskInput(
      startedAt = LocalDateTime.now().minusMinutes(Random.nextInt(1000)),
      duration = Duration.ofMinutes(Random.between(1, 8 * 60)),
      volume = Some(Random.between(1, 10)),
      comment = Some(Random.nextString(Random.between(1, 64)))
    )
  }
}
