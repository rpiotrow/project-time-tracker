package io.github.rpiotrow.ptt.api

import eu.timepit.refined.auto._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty

package object model {

  /*
    TODO: consider scala-newtype to have refined without runtime overhead
    see: https://github.com/estatico/scala-newtype
    see: https://blog.softwaremill.com/a-simple-trick-to-improve-type-safety-of-your-scala-code-ba80559ca092
   */
  type ProjectId = String Refined NonEmpty

  object ProjectId {
    private[api] val example: ProjectId           = "awesome-project-one"
    private[api] val exampleList: List[ProjectId] = List("awesome-project-one", "another-project")
  }

}
