package io.github.rpiotrow.projecttimetracker.api.param

import enumeratum._

sealed trait OrderingDirection extends EnumEntry

object OrderingDirection extends Enum[OrderingDirection] with CirceEnum[OrderingDirection] {
  val values = findValues
  case object Ascending extends OrderingDirection
  case object Descending extends OrderingDirection
}
