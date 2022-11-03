name := "finagle-prometheus"

lazy val commonSettings = Seq(
  organization := "io.github.martinffx",
  scalaVersion := "2.12.10",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
)

crossScalaVersions := Seq("2.12.17", "2.13.10")

organization := "io.github.martinffx"

val finagleVersion = "22.7.0"
val specs2Version = "4.17.0"
val prometheusVersion = "0.16.0"

libraryDependencies ++= Seq(
  "com.twitter" %% "finagle-core" % finagleVersion,
  "com.twitter" %% "finagle-http" % finagleVersion,
  "com.twitter" %% "finagle-stats" % finagleVersion,
  "io.prometheus" % "simpleclient" % prometheusVersion,
  "io.prometheus" % "simpleclient_common" % prometheusVersion,
  "org.specs2" %% "specs2-core" % specs2Version % "test",
  "org.specs2" %% "specs2-mock" % specs2Version % "test"
)

Test / parallelExecution := true
ThisBuild / parallelExecution := false
ThisBuild / publishArtifact := true

lazy val core = (project in file("."))
  .settings(commonSettings: _*)

lazy val examples = (project in file("examples"))
  .settings(commonSettings: _*)
  .dependsOn(core)
  .settings()

lazy val root = project.aggregate(core, examples)

homepage := Some(url("https://github.com/martinffx/finagle-prometheus"))
