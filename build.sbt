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
  .settings(commonSettings)

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
