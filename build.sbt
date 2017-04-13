import Dependencies._

val commonSettings = Seq(
  organization := "com.github.TanUkkii007",
  homepage := Some(url("https://github.com/TanUkkii007/")),
  scalaVersion := "2.12.1",
  crossScalaVersions := Seq("2.11.8", "2.12.1"),
  scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-encoding", "UTF-8", "-language:implicitConversions", "-language:postfixOps"),
  licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
)

val noPublishSettings = Seq(
  publish := (), 
  publishArtifact in Compile := false
)

lazy val root = project.in(file("."))
  .settings(noPublishSettings)
  .aggregate(migrationProxyCore, migrationProxyRecorder, migrationProxyExample)

lazy val migrationProxyCore = project.in(file("migration-proxy-core"))
  .settings(commonSettings)
  .settings(
    name := "migration-proxy-core",
    libraryDependencies ++= Seq(
      Akka.actor,
      Akka.testkit % "test",
      AkkaHttp.http
    )
  )

lazy val migrationProxyRecorder = project.in(file("migration-proxy-recorder"))
  .settings(commonSettings)
  .dependsOn(migrationProxyCore)
  .settings(
    name := "migration-proxy-recorder"
  )

lazy val migrationProxyExample = project.in(file("migration-proxy-example"))
  .settings(commonSettings)
  .dependsOn(migrationProxyCore)
  .settings(
    name := "migration-proxy-example",
    libraryDependencies ++= Seq(
      ScalaTest.scalatest
    )
  )
