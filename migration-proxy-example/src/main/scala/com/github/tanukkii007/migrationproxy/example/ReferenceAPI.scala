package com.github.tanukkii007.migrationproxy.example

import akka.http.scaladsl.server.{Directives, Route}


trait ReferenceAPI extends Directives {

  val route: Route = pathPrefix("example") {
    pathEndOrSingleSlash {
      get {
        parameters("index".as[Long]) { index =>
          complete(index.toString)
        }
      } ~
        post {
          entity(as[String]) { message =>
            complete(message)
          }
        }
    } ~ path(LongNumber) { number =>
      get {
        complete(number.toString)
      }
    }
  }
}

object ReferenceAPI extends ReferenceAPI