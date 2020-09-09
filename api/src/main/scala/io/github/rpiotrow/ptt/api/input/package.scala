package io.github.rpiotrow.ptt.api

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._

package object input {

  type BearerToken = String Refined MatchesRegex["^[A-Za-z0-9-_.]+$"]

}
