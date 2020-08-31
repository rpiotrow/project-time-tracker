package io.github.rpiotrow.ptt.api.param

import enumeratum._

sealed trait ProjectOrderingField extends EnumEntry

object ProjectOrderingField extends Enum[ProjectOrderingField] with CirceEnum[ProjectOrderingField] {
  val values = findValues
  case object CreatedAt         extends ProjectOrderingField
  case object LastAddDurationAt extends ProjectOrderingField
}
