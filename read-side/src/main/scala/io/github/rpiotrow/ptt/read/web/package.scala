package io.github.rpiotrow.ptt.read

import zio.blocking.Blocking
import zio.clock.Clock
import zio.Has

package object web {
  type WebEnv = Clock with Blocking

  type Routes = Has[Routes.Service]
  type Server = Has[Server.Service]
}
