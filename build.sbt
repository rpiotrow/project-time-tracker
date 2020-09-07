lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .aggregate(api, gateway, `read-side`, `write-side`, `e2e-tests`)

lazy val apiLibraryDependencies = Seq(
  "eu.timepit"                  %% "refined"              % Versions.refined,
  "io.circe"                    %% "circe-generic"        % Versions.circe,
  "io.circe"                    %% "circe-refined"        % Versions.circe,
  "io.circe"                    %% "circe-generic-extras" % Versions.circe,
  "com.beachape"                %% "enumeratum"           % Versions.enumeratum,
  "com.beachape"                %% "enumeratum-circe"     % Versions.enumeratum,
  "com.softwaremill.sttp.tapir" %% "tapir-core"           % Versions.tapir,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe"     % Versions.tapir,
  "com.softwaremill.sttp.tapir" %% "tapir-enumeratum"     % Versions.tapir,
  "com.softwaremill.sttp.tapir" %% "tapir-refined"        % Versions.tapir,
  "com.softwaremill.sttp.tapir" %% "tapir-cats"           % Versions.tapir
)

lazy val api = project
  .settings(commonSettings, libraryDependencies ++= apiLibraryDependencies)

lazy val gateway = project
  .configs(IntegrationTest)
  .settings(
    commonSettings,
    Defaults.itSettings,
    libraryDependencies ++= apiLibraryDependencies ++ Seq(
      "com.typesafe.akka"           %% "akka-http"                  % Versions.akkaHttp,
      "com.typesafe.akka"           %% "akka-stream"                % Versions.akkaStream,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-akka-http" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"         % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml"   % Versions.tapir,
      "com.github.pureconfig"       %% "pureconfig"                 % Versions.pureConfig,
      "com.pauldijou"               %% "jwt-circe"                  % Versions.jwt,
      "ch.qos.logback"               % "logback-classic"            % Versions.logback,
      "org.scalamock"               %% "scalamock"                  % Versions.scalamock       % Test,
      "org.scalactic"               %% "scalactic"                  % Versions.scalatest       % "test, it",
      "org.scalatest"               %% "scalatest"                  % Versions.scalatest       % "test, it",
      "com.typesafe.akka"           %% "akka-http-testkit"          % Versions.akkaHttpTestkit % "test, it",
      "com.typesafe.akka"           %% "akka-stream-testkit"        % Versions.akkaStream      % "test, it",
      "org.mock-server"              % "mockserver-netty"           % Versions.mockserver      % IntegrationTest
    )
  )
  .dependsOn(api)

lazy val `read-side` = project
  .configs(IntegrationTest)
  .settings(
    commonSettings,
    Defaults.itSettings,
    libraryDependencies ++= apiLibraryDependencies ++ Seq(
      "dev.zio"                     %% "zio"                             % Versions.zio,
      "dev.zio"                     %% "zio-config"                      % Versions.zioConfig,
      "dev.zio"                     %% "zio-config-magnolia"             % Versions.zioConfig,
      "dev.zio"                     %% "zio-config-typesafe"             % Versions.zioConfig,
      "dev.zio"                     %% "zio-interop-cats"                % Versions.zioInteropCats,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"             % Versions.tapir,
      "org.http4s"                  %% "http4s-blaze-server"             % Versions.http4s,
      "org.http4s"                  %% "http4s-circe"                    % Versions.http4s,
      "org.http4s"                  %% "http4s-dsl"                      % Versions.http4s,
      "org.tpolecat"                %% "doobie-core"                     % Versions.doobie,
      "org.tpolecat"                %% "doobie-postgres"                 % Versions.doobie,
      "org.tpolecat"                %% "doobie-hikari"                   % Versions.doobie,
      "org.tpolecat"                %% "doobie-quill"                    % Versions.doobie,
      "ch.qos.logback"               % "logback-classic"                 % Versions.logback,
      "org.scalactic"               %% "scalactic"                       % Versions.scalatest      % "test, it",
      "org.scalatest"               %% "scalatest"                       % Versions.scalatest      % "test, it",
      "com.softwaremill.diffx"      %% "diffx-scalatest"                 % Versions.diffx          % "test, it",
      "org.scalamock"               %% "scalamock"                       % Versions.scalamock      % Test,
      "com.dimafeng"                %% "testcontainers-scala-scalatest"  % Versions.testContainers % IntegrationTest,
      "com.dimafeng"                %% "testcontainers-scala-postgresql" % Versions.testContainers % IntegrationTest
    )
  )
  .dependsOn(api)

lazy val `write-side` = project
  .configs(IntegrationTest)
  .settings(
    commonSettings,
    Defaults.itSettings,
    libraryDependencies ++= apiLibraryDependencies ++ Seq(
      "org.typelevel"               %% "cats-core"                       % Versions.cats,
      "com.github.pureconfig"       %% "pureconfig"                      % Versions.pureConfig,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"             % Versions.tapir,
      "org.http4s"                  %% "http4s-blaze-server"             % Versions.http4s,
      "org.http4s"                  %% "http4s-circe"                    % Versions.http4s,
      "org.http4s"                  %% "http4s-dsl"                      % Versions.http4s,
      "org.tpolecat"                %% "doobie-core"                     % Versions.doobie,
      "org.tpolecat"                %% "doobie-postgres"                 % Versions.doobie,
      "org.tpolecat"                %% "doobie-hikari"                   % Versions.doobie,
      "org.tpolecat"                %% "doobie-quill"                    % Versions.doobie,
      "ch.qos.logback"               % "logback-classic"                 % Versions.logback,
      "org.scalactic"               %% "scalactic"                       % Versions.scalatest      % "test, it",
      "org.scalatest"               %% "scalatest"                       % Versions.scalatest      % "test, it",
      "com.softwaremill.diffx"      %% "diffx-scalatest"                 % Versions.diffx          % "test, it",
      "org.scalamock"               %% "scalamock"                       % Versions.scalamock      % Test,
      "com.dimafeng"                %% "testcontainers-scala-scalatest"  % Versions.testContainers % IntegrationTest,
      "com.dimafeng"                %% "testcontainers-scala-postgresql" % Versions.testContainers % IntegrationTest
    )
  )
  .dependsOn(api)

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

addCommandAlias("checks", ";test;it:test")
addCommandAlias("runAll", ";project gateway;bgRun;project read-side;bgRun;project write-side;run")
