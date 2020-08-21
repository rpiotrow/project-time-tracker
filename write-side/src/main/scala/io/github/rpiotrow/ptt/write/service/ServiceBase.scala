package io.github.rpiotrow.ptt.write.service

import cats.data.EitherT
import cats.implicits._
import io.github.rpiotrow.ptt.write.repository.DBResult

trait ServiceBase {

  protected def ifExists[A](option: Option[A], message: String): EitherT[DBResult, EntityNotFound, A] = {
    option match {
      case Some(entity) => EitherT.right[EntityNotFound](entity.pure[DBResult])
      case None         =>
        EitherT.left[A](EntityNotFound(message).pure[DBResult])
    }
  }

}
