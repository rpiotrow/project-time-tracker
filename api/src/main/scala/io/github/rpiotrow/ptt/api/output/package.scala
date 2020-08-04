package io.github.rpiotrow.ptt.api

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url

package object output {

  type LocationHeader = String Refined Url

}
