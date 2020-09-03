package io.github.rpiotrow.ptt.api.model

import cats.data.NonEmptyList

case class NonEmptyUserIdList(list: NonEmptyList[UserId]) extends AnyVal {
  def toList: List[UserId] = list.toList
}

object NonEmptyUserIdList {

  def of(head: UserId, tail: UserId*): NonEmptyUserIdList =
    NonEmptyUserIdList(NonEmptyList.of[UserId](head, tail: _*))

  private[api] val example = NonEmptyUserIdList(
    NonEmptyList(
      UserId("907e591f-44cd-4c70-8ea9-707e30ada160"),
      List(UserId("40c728cc-8540-4ba0-81f8-c2834dd1098f"), UserId("b5d09d4c-06f0-44d8-81e6-e4da7ece5e0"))
    )
  )
}
