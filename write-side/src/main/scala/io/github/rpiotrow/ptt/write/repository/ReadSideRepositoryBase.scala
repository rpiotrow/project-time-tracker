package io.github.rpiotrow.ptt.write.repository

import org.slf4j.LoggerFactory

trait ReadSideRepositoryBase {

  private val logger = LoggerFactory.getLogger("ReadSideRepository")

  protected def logIfNotUpdated(message: String)(result: Long): Unit =
    result match {
      case 1 => ()
      case _ => logger.warn("Read model update failure: " + message)
    }

}
