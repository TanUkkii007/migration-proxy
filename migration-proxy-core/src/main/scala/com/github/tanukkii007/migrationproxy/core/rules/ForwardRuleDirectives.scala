package com.github.tanukkii007.migrationproxy.core.rules

import akka.http.javadsl.model.headers.TimeoutAccess
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.Uri.Authority


trait ForwardRuleDirectives {
  import BasicRuleDirectives._
  import FutureRuleDirectives._

  def forwardTo(host: String): RuleDirective1[HttpResponse] = {
    extractRequest.flatMap { request =>
      extractMaterializer.flatMap { implicit materializer =>
        extractActorSystem.flatMap { implicit system =>
          val authority = Authority.parse(host)
          val filteredHeader = request.headers.filter {
            case _: TimeoutAccess => false
            case _ => true
          }
          val forwardRequest = request.copy(uri = request.uri.copy(authority = authority), headers = filteredHeader)
          onSuccess(Http().singleRequest(forwardRequest))
        }
      }
    }
  }


}
