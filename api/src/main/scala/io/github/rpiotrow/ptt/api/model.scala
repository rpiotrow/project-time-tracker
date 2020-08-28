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
      UUID.fromString("500cd26f-4c42-4051-9600-2e4ad2fab3b8"),
      List(UUID.fromString("33f7d94f-a223-4dc6-afd9-9246f6204807"))
    )
  }

}
