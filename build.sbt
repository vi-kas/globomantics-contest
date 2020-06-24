
lazy val root = (project in file("."))
  .settings(
    name := "globomantics-contest-service",
    scalaVersion := "2.12.8",
    organization := "com.globomantics",
    description := "Globomantics Contest Platform",
    version := "0.1",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-slf4j" % "2.6.6",
      "ch.qos.logback"     % "logback-classic" % "1.2.3",
      "com.typesafe.akka" %% "akka-http"   % "10.1.10",
      "com.typesafe.akka" %% "akka-http-core" % "10.1.10",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.10",
      "com.typesafe.akka" %% "akka-stream" % "2.6.6",
      "com.typesafe.akka" %% "akka-actor-typed" % "2.6.6",
      "com.typesafe.akka" %% "akka-http-testkit" % "10.1.10" % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.6.6" % Test,
      "org.scalatest"     %% "scalatest" % "3.0.8" % Test
    )
  )