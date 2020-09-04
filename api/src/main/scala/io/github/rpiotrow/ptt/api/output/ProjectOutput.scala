package io.github.rpiotrow.ptt.api.output

import java.time.{Duration, LocalDateTime}

import eu.timepit.refined.auto._
import cats.implicits._
import io.github.rpiotrow.ptt.api.model.{ProjectId, TaskId, UserId}

case class ProjectOutput(
  projectId: ProjectId,
  createdAt: LocalDateTime,
  deletedAt: Option[LocalDateTime],
  owner: UserId,
  durationSum: Duration,
  tasks: List[TaskOutput]
)

object ProjectOutput {
  private[api] val example = ProjectOutput(
    projectId = "awesome-project-one",
    owner = UserId("c5026130-043c-46be-9966-3299acf924e2"),
    createdAt = LocalDateTime.now(),
    deletedAt = None,
    durationSum = Duration.ofMinutes(45),
    tasks = List(
      TaskOutput.example,
      TaskOutput.example.copy(
        taskId = TaskId("71a0253a-4c8c-4f74-9d75-c745b004c703"),
        duration = Duration.ofMinutes(15),
        comment = "Second task".some
      ),
      TaskOutput.example.copy(
        taskId = TaskId("26797cb6-a5ee-41ba-b817-37114f3e4e8d"),
        deletedAt = LocalDateTime.now().some,
        comment = "Deleted task".some
      )
    )
  )
}
