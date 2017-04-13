package com.github.tanukkii007.migrationproxy.example

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.github.tanukkii007.migrationproxy.core.ProxyServer
import com.github.tanukkii007.migrationproxy.core.reporter.DefaultReporter

import scala.util.Try

object Main extends App with Rules {

  val referenceAPIBinding = {
    implicit val referenceSystem = ActorSystem("reference")
    implicit val mat = ActorMaterializer()
    Http().bindAndHandle(ReferenceAPI.route, "localhost", 8081)
  }

  val checkTargetAPIBinding = {
    implicit val checkTargetSystem = ActorSystem("checkTarget")
    implicit val mat = ActorMaterializer()
    Http().bindAndHandle(CheckTargetAPI.route, "localhost", 8082)
  }


  val proxyBinding = {
    implicit val system = ActorSystem("proxy")
    implicit val mat = ActorMaterializer()

    val client: Flow[HttpRequest, Try[HttpResponse], NotUsed] = {
      Flow[HttpRequest].map(_ -> 1).via(Http().superPool[Int]()).map(_._1)
    }

    val proxy = new ProxyServer()

    system.actorOf(DefaultReporter.props)

    Http().bindAndHandle(proxy.bindableFlow(rule, "localhost:8081", client), "localhost", 8080)
  }

}

