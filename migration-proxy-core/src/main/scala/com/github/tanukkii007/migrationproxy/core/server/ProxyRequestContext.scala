/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 * 2017- modified by Yusuke Yasuda
 */

package com.github.tanukkii007.migrationproxy.core.server

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Rejection
import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import com.github.tanukkii007.migrationproxy.core.rules.RuleResult
import com.github.tanukkii007.migrationproxy.core.rules.RuleResult.{Ignored, Passed}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

/*
 * ported from akka.http.scaladsl.server.RequestContextImpl
 */
case class ProxyRequestContext(request: HttpRequest,
                               expectedResponse: HttpResponse,
                               unmatchedPath: Uri.Path,
                               implicit val executionContext: ExecutionContextExecutor,
                               implicit val materializer:     Materializer,
                               log: LoggingAdapter) {

  def ignore: Future[Ignored] = FastFuture.successful(RuleResult.Ignored(request))

  def complete: Future[RuleResult] = FastFuture.successful(RuleResult.Passed(request, expectedResponse))

  def reject(rejections: Rejection*): Future[RuleResult] =
    FastFuture.successful(RuleResult.Rejected(rejections.toList))

  def fail(error: Throwable): Future[RuleResult] =
    FastFuture.successful(RuleResult.Failed(request, expectedResponse, error))

  def withRequest(request: HttpRequest): ProxyRequestContext =
    if (request != this.request) copy(request = request) else this

  def withExecutionContext(executionContext: ExecutionContextExecutor): ProxyRequestContext =
    if (executionContext != this.executionContext) copy(executionContext = executionContext) else this

  def withMaterializer(materializer: Materializer): ProxyRequestContext =
    if (materializer != this.materializer) copy(materializer = materializer) else this

  def withLog(log: LoggingAdapter): ProxyRequestContext =
    if (log != this.log) copy(log = log) else this

  def mapRequest(f: HttpRequest ⇒ HttpRequest): ProxyRequestContext =
    copy(request = f(request))

  def withUnmatchedPath(path: Uri.Path): ProxyRequestContext =
    if (path != unmatchedPath) copy(unmatchedPath = path) else this

  def mapUnmatchedPath(f: Uri.Path ⇒ Uri.Path): ProxyRequestContext =
    copy(unmatchedPath = f(unmatchedPath))
}