package io.github.rpiotrow.ptt.read

import zio._
import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import zio.config.typesafe.TypesafeConfig

package object configuration {

  object Configuration {
    val appConfigurationDescriptor: ConfigDescriptor[AppConfiguration] =
      descriptor[AppConfiguration]
    val live: Layer[Throwable, Config[AppConfiguration]]               =
      TypesafeConfig.fromDefaultLoader(appConfigurationDescriptor)

  }

}
