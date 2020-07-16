package io.github.rpiotrow.projecttimetracker.api

import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.numeric.{LessEqual, Positive}

package object param {

  type PageNumber = Int Refined Positive
  type PageSize = Int Refined (Positive And LessEqual[1000])

}
