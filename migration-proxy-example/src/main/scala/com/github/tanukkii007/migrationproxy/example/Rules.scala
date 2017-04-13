package com.github.tanukkii007.migrationproxy.example

import akka.http.scaladsl.unmarshalling.Unmarshal
import com.github.tanukkii007.migrationproxy.core.rules.Rule
import com.github.tanukkii007.migrationproxy.core.server.AllRuleDirectives._
import org.scalatest.Matchers

trait Rules extends Matchers {
  val rule: Rule = pathPrefix("example") {
    pathEndOrSingleSlash {
      get {
        parameters("index".as[Long]) { index =>
          forwardTo("localhost:8082") { response =>
            extractMaterializer { implicit mat =>
              onSuccess(Unmarshal(response).to[String]) { indexString =>
                indexString.toLong should be(index)
                complete
              }
            }
          }
        }
      }
    } ~ path(LongNumber) { number =>
      get {
        forwardTo("localhost:8082") { response =>
          println(response)
          complete
        }
      }
    }
  }
}
