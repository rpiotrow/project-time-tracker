package io.github.rpiotrow.ptt.api

import java.util.UUID

import cats.data.NonEmptyList
import eu.timepit.refined.auto._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty

object model {

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
  type UserId = UUID

  object UserId {
    private[api] val example: UserId = UUID.fromString("c5026130-043c-46be-9966-3299acf924e2")
  }

  type ProjectId = String Refined NonEmpty

  object ProjectId {
    private[api] val example: ProjectId           = "awesome-project-one"
    private[api] val exampleList: List[ProjectId] = List("awesome-project-one", "another-project")
  }

  type TaskId = UUID

  object TaskId {
    private[api] val example: TaskId = UUID.fromString("71a0253a-4c8c-4f74-9d75-c745b004c703")
  }

  type NonEmptyUserIdList = NonEmptyList[UserId]

  object NonEmptyUserIdList {
    private[api] val example = NonEmptyList(
      UUID.fromString("907e591f-44cd-4c70-8ea9-707e30ada160"),
      List(
        UUID.fromString("40c728cc-8540-4ba0-81f8-c2834dd1098f"),
        UUID.fromString("b5d09d4c-06f0-44d8-81e6-e4da7ece5e0")
      )
    )
  }

}
