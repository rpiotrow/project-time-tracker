package io.github.rpiotrow.ptt.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.semiauto._
import io.github.rpiotrow.ptt.api.model.{NonEmptyUserIdList, TaskId, UserId}

object CirceMappings {

  implicit val userIdEncoder: Encoder[UserId] = deriveUnwrappedEncoder
  implicit val userIdDecoder: Decoder[UserId] = deriveUnwrappedDecoder

  implicit val taskIdEncoder: Encoder[TaskId] = deriveUnwrappedEncoder
  implicit val taskIdDecoder: Decoder[TaskId] = deriveUnwrappedDecoder

  implicit val nonEmptyUserIdListEncoder: Encoder[NonEmptyUserIdList] = deriveUnwrappedEncoder
  implicit val nonEmptyUserIdListDecoder: Decoder[NonEmptyUserIdList] = deriveUnwrappedDecoder

}
