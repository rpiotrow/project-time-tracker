package io.github.rpiotrow.ptt.api

import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.numeric.{LessEqual, NonNegative, Positive}

package object param {

  type PageNumber = Int Refined NonNegative
  type PageSize   = Int Refined (Positive And LessEqual[1000])

}
