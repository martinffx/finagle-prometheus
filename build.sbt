name := "finagle-prometheus"

lazy val commonSettings = Seq(
  organization := "io.github.martinffx",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
)

ThisBuild / crossScalaVersions := Seq("2.12.17", "2.13.10")

ThisBuild / organization := "io.github.martinffx"
ThisBuild / description := "Publish Finagle StatsCollector stats via Prometheus"
ThisBuild / homepage := Some(
  url("https://github.com/martinffx/finagle-prometheus")
)

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/martinffx/finagle-prometheus"),
    "scm:git@github.com:martinffx/finagle-prometheus.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "martinffx ",
    name = "Martin C. Richards",
    email = "martinffx@gmail.com",
    url = url("https://martinrichards.me")
  )
)

ThisBuild / credentials += Credentials(
  Path.userHome / ".sbt" / "1.0" / "sonatype_credentials"
)

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  // For accounts created after Feb 2021:
  // val nexus = "https://s01.oss.sonatype.org/"
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true

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
ThisBuild / publishArtifact := false
ThisBuild / publishConfiguration := publishConfiguration.value.withOverwrite(
  true
)
ThisBuild / releaseCrossBuild := true
ThisBuild / releasePublishArtifactsAction := PgpKeys.publishSigned.value

lazy val core = (project in file("."))
  .settings(commonSettings: _*)

lazy val examples = (project in file("examples"))
  .settings(commonSettings: _*)
  .dependsOn(core)
  .settings()

lazy val root = project.aggregate(core, examples)
