package io.github.rpiotrow.ptt.api.write

import zio.Has

package object web {
  type Routes = Has[Routes.Service]
  type Server = Has[Server.Service]
}
