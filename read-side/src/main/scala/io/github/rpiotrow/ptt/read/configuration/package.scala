package io.github.rpiotrow.ptt.read

import zio._
import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import zio.config.typesafe.TypesafeConfig

package object configuration {

  object Configuration {
    val appConfigurationDescriptor: ConfigDescriptor[DatabaseConfiguration] =
      descriptor[DatabaseConfiguration]
    val live: Layer[Throwable, Config[DatabaseConfiguration]]               =
      TypesafeConfig.fromDefaultLoader(appConfigurationDescriptor)
  }

}
