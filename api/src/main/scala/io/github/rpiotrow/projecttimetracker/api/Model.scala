package io.github.rpiotrow.projecttimetracker.api

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import eu.timepit.refined.collection.NonEmpty

object Model {

  /*
    TODO: consider scala-newtype to have refined without runtime overhead
    see: https://github.com/estatico/scala-newtype
    see: https://blog.softwaremill.com/a-simple-trick-to-improve-type-safety-of-your-scala-code-ba80559ca092

    check also:
    libraryDependencies += "io.circe" %% "circe-generic-extras" % version
    import io.circe.generic.extras.semiauto._
    case class UserId(id: String) extends AnyVal
    implicit val encoder: Encoder[UserId] = deriveUnwrappedEncoder
    implicit val decoder: Decoder[UserId] = deriveUnwrappedDecoder

   */
  type UserId    = String Refined Uuid
  type ProjectId = String Refined NonEmpty
  type TaskId    = String Refined Uuid

}
