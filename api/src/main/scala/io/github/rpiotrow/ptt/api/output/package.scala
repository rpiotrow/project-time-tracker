package io.github.rpiotrow.ptt.api

import io.github.rpiotrow.ptt.api.model.{ProjectId, TaskId}

package object output {

  type LocationHeader = java.net.URL

  object LocationHeader {
    private[api] val exampleProjectLocation = new LocationHeader(s"http://localhost:8080/projects/${ProjectId.example}")
    private[api] val exampleTaskLocation    = new LocationHeader(
      s"http://localhost:8080/projects/${ProjectId.example}/tasks/${TaskId.example}"
    )
  }
}
