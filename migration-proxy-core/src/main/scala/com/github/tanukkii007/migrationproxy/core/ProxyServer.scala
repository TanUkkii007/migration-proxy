package com.github.tanukkii007.migrationproxy.core

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.javadsl.model.headers.TimeoutAccess
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Authority
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}
import com.github.tanukkii007.migrationproxy.core.check.CheckResultAggregator
import com.github.tanukkii007.migrationproxy.core.rules.Rule
import com.github.tanukkii007.migrationproxy.core.server.{ProxyExceptionHandler, ProxyRequestContext}
import com.github.tanukkii007.migrationproxy.core.rules.ExecutionRuleDirectives

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.Try


class ProxyServer(implicit system: ActorSystem, mat: Materializer) {

  val aggregator = system.actorOf(CheckResultAggregator.props, "check-result-aggregator")

  def bindableFlow(rule: Rule, proxyTarget: String, httpClient: Flow[HttpRequest, Try[HttpResponse], NotUsed]): Flow[HttpRequest, HttpResponse, NotUsed] = {
    import mat.executionContext
    import ExecutionRuleDirectives._

    val sealedRule = handleExceptions(ProxyExceptionHandler.seal()) {
      rule
    }
    createProxyRequestContextFlow(proxyTarget, httpClient)
      .mapAsync(1)(rc => sealedRule(rc).map { result =>
        aggregator ! CheckResultAggregator.ReportResult(result)
        result
      }.map(rc -> _))
      .map(_._1.expectedResponse)
  }

  private def createProxyRequestContextFlow(proxyTarget: String, client: Flow[HttpRequest, Try[HttpResponse], NotUsed])
                                           (implicit ec: ExecutionContextExecutor = null): Flow[HttpRequest, ProxyRequestContext, NotUsed] = {
    implicit val effectiveEc = if (ec ne null) ec else mat.executionContext
    Flow[HttpRequest].flatMapConcat { request =>
      val filteredHeader = request.headers.filter {
        case _: TimeoutAccess => false
        case _ => true
      }
      val authority = Authority.parse(proxyTarget)
      val forwardRequest = request.copy(uri = request.uri.copy(authority = authority), headers = filteredHeader)
      Source.single(forwardRequest).via(client).map { response =>
        ProxyRequestContext(request, response.get, request.uri.path, ec, mat, system.log)
      }
    }
  }
}
