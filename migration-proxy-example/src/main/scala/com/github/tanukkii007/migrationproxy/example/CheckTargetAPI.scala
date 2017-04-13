package com.github.tanukkii007.migrationproxy.example

import akka.http.scaladsl.server.{Directives, Route}


trait CheckTargetAPI extends Directives {

  val route: Route = pathPrefix("example") {
    pathEndOrSingleSlash {
      get {
        parameters("index".as[Long]) { index =>
          complete((index * -1).toString)
        }
      } ~
        post {
          entity(as[String]) { message =>
            complete(message)
          }
        }
    }
    // Not implemented
    /* ~ path(LongNumber) { number =>
      get {
        complete(number.toString)
      }
    }*/
  }
}

object CheckTargetAPI extends CheckTargetAPI