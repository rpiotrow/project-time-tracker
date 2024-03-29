enablePlugins(GitVersioning)

lazy val root = project
  .in(file("."))
  .settings(commonSettings, name := "project-time-tracker", publish / skip := true)
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
  .settings(
    name := "project-time-tracker-api",
    commonSettings,
    publish / skip := true,
    libraryDependencies ++= apiLibraryDependencies,
    exportJars := true
  )

lazy val gateway = project
  .configs(IntegrationTest)
  .settings(
    name := "project-time-tracker-gateway",
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
      "org.scalamock"               %% "scalamock"                  % Versions.scalamock  % Test,
      "org.scalactic"               %% "scalactic"                  % Versions.scalatest  % "test, it",
      "org.scalatest"               %% "scalatest"                  % Versions.scalatest  % "test, it",
      "com.typesafe.akka"           %% "akka-http-testkit"          % Versions.akkaHttp   % "test, it",
      "com.typesafe.akka"           %% "akka-stream-testkit"        % Versions.akkaStream % "test, it",
      "org.mock-server"              % "mockserver-netty"           % Versions.mockserver % IntegrationTest
    ),
    commonDockerSettings,
    dockerExposedPorts := Seq(8080),
    packageName := "ptt-gateway"
  )
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .dependsOn(api)

lazy val `read-side` = project
  .configs(IntegrationTest)
  .settings(
    name := "project-time-tracker-read-side",
    commonSettings,
    Defaults.itSettings,
    IntegrationTest / test / fork := true,
    libraryDependencies ++= apiLibraryDependencies ++ Seq(
      "dev.zio"                     %% "zio"                             % Versions.zio,
      "dev.zio"                     %% "zio-config"                      % Versions.zioConfig,
      "dev.zio"                     %% "zio-config-magnolia"             % Versions.zioConfig,
      "dev.zio"                     %% "zio-config-typesafe"             % Versions.zioConfig,
      "dev.zio"                     %% "zio-interop-cats"                % Versions.zioInteropCats,
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http4s-server"         % Versions.tapir,
      "org.http4s"                  %% "http4s-blaze-server"             % Versions.http4s,
      "org.http4s"                  %% "http4s-circe"                    % Versions.http4s,
      "org.http4s"                  %% "http4s-dsl"                      % Versions.http4s,
      "org.postgresql"               % "postgresql"                      % Versions.postgresql,
      "io.getquill"                 %% "quill-jdbc-zio"                  % Versions.quill,
      "ch.qos.logback"               % "logback-classic"                 % Versions.logback,
      "org.scalactic"               %% "scalactic"                       % Versions.scalatest      % "test, it",
      "org.scalatest"               %% "scalatest"                       % Versions.scalatest      % "test, it",
      "org.tpolecat"                %% "doobie-core"                     % Versions.doobie         % "test, it",
      "com.softwaremill.diffx"      %% "diffx-scalatest-should"          % Versions.diffx          % "test, it",
      "org.scalamock"               %% "scalamock"                       % Versions.scalamock      % Test,
      "com.dimafeng"                %% "testcontainers-scala-scalatest"  % Versions.testContainers % IntegrationTest,
      "com.dimafeng"                %% "testcontainers-scala-postgresql" % Versions.testContainers % IntegrationTest
    ),
    commonDockerSettings,
    dockerExposedPorts := Seq(8081),
    packageName := "ptt-read-side"
  )
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .dependsOn(api)

lazy val `write-side` = project
  .configs(IntegrationTest)
  .settings(
    name := "project-time-tracker-write-side",
    commonSettings,
    Defaults.itSettings,
    IntegrationTest / test / fork := true,
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
      "com.softwaremill.diffx"      %% "diffx-scalatest-should"          % Versions.diffx          % "test, it",
      "org.scalamock"               %% "scalamock"                       % Versions.scalamock      % Test,
      "org.typelevel"               %% "cats-effect-testing-scalatest"   % Versions.catsScalatest  % Test,
      "com.dimafeng"                %% "testcontainers-scala-scalatest"  % Versions.testContainers % IntegrationTest,
      "com.dimafeng"                %% "testcontainers-scala-postgresql" % Versions.testContainers % IntegrationTest
    ),
    commonDockerSettings,
    dockerExposedPorts := Seq(8082),
    packageName := "ptt-write-side"
  )
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .dependsOn(api)

lazy val EndToEndTest = config("e2e") extend (Runtime)
lazy val e2eSettings  =
  inConfig(EndToEndTest)(Defaults.testSettings) ++
    Seq(
      EndToEndTest / fork := false,
      EndToEndTest / parallelExecution := false,
      EndToEndTest / scalaSource := baseDirectory.value / "src/e2e/scala"
    )

lazy val `e2e-tests` = project
  .configs(EndToEndTest)
  .settings(
    name := "project-time-tracker-e2e-tests",
    commonSettings,
    e2eSettings,
    publish / skip := true,
    libraryDependencies ++= apiLibraryDependencies ++ Seq(
      "com.github.pureconfig"       %% "pureconfig"             % Versions.pureConfig % "e2e",
      "com.pauldijou"               %% "jwt-circe"              % Versions.jwt        % "e2e",
      "org.scalactic"               %% "scalactic"              % Versions.scalatest  % "e2e",
      "org.scalatest"               %% "scalatest"              % Versions.scalatest  % "e2e",
      "com.softwaremill.diffx"      %% "diffx-scalatest-should" % Versions.diffx      % "e2e",
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-client"      % Versions.tapir      % "e2e"
    )
  )
  .dependsOn(api)

lazy val commonSettings = Seq(
  organization := "io.github.rpiotrow",
  scalaVersion := "2.13.6",
  addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.2" cross CrossVersion.full),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
  scalacOptions ++= compilerOptions
)

lazy val commonDockerSettings =
  Seq(dockerBaseImage := "openjdk:11-jre-slim", dockerAutoremoveMultiStageIntermediateImages := false)

lazy val compilerOptions = Seq(
  "-target:jvm-11",
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Xfatal-warnings"
)

addCommandAlias("compileAll", "compile;Test/compile;IntegrationTest/compile;e2e-tests/EndToEndTest/compile")
addCommandAlias("checks", "test;IntegrationTest/test")
addCommandAlias("runAll", "project gateway;bgRun;project read-side;bgRun;project write-side;run")
