lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .aggregate(api, gateway, `read-side`, `write-side`, `e2e-tests`)

lazy val api = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "io.circe"                    %% "circe-generic"    % Versions.circe,
      "io.circe"                    %% "circe-refined"    % Versions.circe,
      "com.beachape"                %% "enumeratum"       % Versions.enumeratum,
      "com.beachape"                %% "enumeratum-circe" % Versions.enumeratum,
      "com.softwaremill.sttp.tapir" %% "tapir-core"       % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-enumeratum" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-refined"    % Versions.tapir
    )
  )

lazy val gateway = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.http4s"                  %% "http4s-blaze-server"      % Versions.http4s,
      "org.http4s"                  %% "http4s-circe"             % Versions.http4s,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"      % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s"  % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"       % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % Versions.tapir,
      "ch.qos.logback"               % "logback-classic"          % Versions.logback
    )
  )
  .dependsOn(api)

lazy val `read-side` = project
  .configs(IntegrationTest)
  .settings(
    commonSettings,
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      "eu.timepit"             %% "refined"                         % Versions.refined,
      "com.beachape"           %% "enumeratum"                      % Versions.enumeratum,
      "com.beachape"           %% "enumeratum-circe"                % Versions.enumeratum,
      "dev.zio"                %% "zio"                             % Versions.zio,
      "dev.zio"                %% "zio-config"                      % Versions.zioConfig,
      "dev.zio"                %% "zio-config-magnolia"             % Versions.zioConfig,
      "dev.zio"                %% "zio-config-typesafe"             % Versions.zioConfig,
      "dev.zio"                %% "zio-interop-cats"                % Versions.zioInteropCats,
      "org.http4s"             %% "http4s-blaze-server"             % Versions.http4s,
      "org.http4s"             %% "http4s-circe"                    % Versions.http4s,
      "org.http4s"             %% "http4s-dsl"                      % Versions.http4s,
      "io.circe"               %% "circe-generic"                   % Versions.circe,
      "org.tpolecat"           %% "doobie-core"                     % Versions.doobie,
      "org.tpolecat"           %% "doobie-postgres"                 % Versions.doobie,
      "org.tpolecat"           %% "doobie-hikari"                   % Versions.doobie,
      "org.tpolecat"           %% "doobie-quill"                    % Versions.doobie,
      "ch.qos.logback"          % "logback-classic"                 % Versions.logback,
      "org.scalactic"          %% "scalactic"                       % Versions.scalatest      % "test, it",
      "org.scalatest"          %% "scalatest"                       % Versions.scalatest      % "test, it",
      "com.softwaremill.diffx" %% "diffx-scalatest"                 % Versions.diffx          % "test, it",
      "org.scalamock"          %% "scalamock"                       % "5.0.0"                 % Test,
      "com.dimafeng"           %% "testcontainers-scala-scalatest"  % Versions.testContainers % IntegrationTest,
      "com.dimafeng"           %% "testcontainers-scala-postgresql" % Versions.testContainers % IntegrationTest
    )
  )
  .dependsOn(api)

lazy val `write-side` = project
  .settings(commonSettings)

lazy val `e2e-tests` = project
  .settings(commonSettings)

lazy val commonSettings = Seq(
  organization := "io.github.rpiotrow",
  name := "project-time-tracker",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.13.1",
  addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.0"),
  scalacOptions ++= compilerOptions
)

lazy val compilerOptions = Seq(
  "-target:jvm-11",
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Xfatal-warnings"
//  "-Ymacro-annotations",
)
