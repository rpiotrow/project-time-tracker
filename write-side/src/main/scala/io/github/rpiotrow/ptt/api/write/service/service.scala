package io.github.rpiotrow.ptt.api.write

import zio.{Has, ZLayer}

package object service {

  type ProjectService = Has[ProjectService.Service]

  type Services = ProjectService

  val liveServices: ZLayer[Any, Throwable, Services] =
    ZLayer.fromFunction(_ => ProjectService.live())
}
