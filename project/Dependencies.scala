import sbt._

object Dependencies {

  object Akka {
    val version = "2.4.17"
    val actor = "com.typesafe.akka" %% "akka-actor" % version
    val stream = "com.typesafe.akka" %% "akka-stream" % version
    val testkit = "com.typesafe.akka" %% "akka-testkit" % version
  }

  object AkkaHttp {
    val version = "10.0.5"
    val core = "com.typesafe.akka" %% "akka-http-core" % version
    val http = "com.typesafe.akka" %% "akka-http" % version
  }

  object ScalaTest {
    val version = "3.0.1"
    val scalatest = "org.scalatest" %% "scalatest" % version
  }

}