/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 * 2017- Modified by Yusuke Yasuda
 */

package com.github.tanukkii007.migrationproxy.core.rules

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.{Rejection, TransformationRejection}
import akka.http.scaladsl.server.util.Tuple
import akka.http.scaladsl.util.FastFuture
import akka.http.scaladsl.util.FastFuture._
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.github.tanukkii007.migrationproxy.core.server.ProxyRequestContext

import scala.collection.immutable
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * Ported from akka.http.scaladsl.server.directives.BasicDirectives
  */
trait BasicRuleDirectives {

  def mapInnerRule(f: Rule ⇒ Rule): RuleDirective0 =
    RuleDirective { inner ⇒ f(inner(())) }

  def mapProxyRequestContext(f: ProxyRequestContext ⇒ ProxyRequestContext): RuleDirective0 =
    mapInnerRule { inner ⇒ ctx ⇒ inner(f(ctx)) }

  def mapRequest(f: HttpRequest ⇒ HttpRequest): RuleDirective0 =
    mapProxyRequestContext(_ mapRequest f)

  def mapRuleResultFuture(f: Future[RuleResult] ⇒ Future[RuleResult]): RuleDirective0 =
    RuleDirective { inner ⇒ ctx ⇒ f(inner(())(ctx)) }

  def mapRuleResult(f: RuleResult ⇒ RuleResult): RuleDirective0 =
    RuleDirective { inner ⇒ ctx ⇒ inner(())(ctx).fast.map(f)(ctx.executionContext) }

  def mapRuleResultWith(f: RuleResult ⇒ Future[RuleResult]): RuleDirective0 =
    RuleDirective { inner ⇒ ctx ⇒ inner(())(ctx).fast.flatMap(f)(ctx.executionContext) }

  def mapRuleResultPF(f: PartialFunction[RuleResult, RuleResult]): RuleDirective0 =
    mapRuleResult(f.applyOrElse(_, identity[RuleResult]))

  def mapRuleResultWithPF(f: PartialFunction[RuleResult, Future[RuleResult]]): RuleDirective0 =
    mapRuleResultWith(f.applyOrElse(_, FastFuture.successful[RuleResult]))

  def recoverRejections(f: immutable.Seq[Rejection] ⇒ RuleResult): RuleDirective0 =
    mapRuleResultPF { case RuleResult.Rejected(rejections) ⇒ f(rejections) }

  def recoverRejectionsWith(f: immutable.Seq[Rejection] ⇒ Future[RuleResult]): RuleDirective0 =
    mapRuleResultWithPF { case RuleResult.Rejected(rejections) ⇒ f(rejections) }

  def mapRejections(f: immutable.Seq[Rejection] ⇒ immutable.Seq[Rejection]): RuleDirective0 =
    recoverRejections(rejections ⇒ RuleResult.Rejected(f(rejections)))

  def pass: RuleDirective0 = RuleDirective.Empty

  def provide[T](value: T): RuleDirective1[T] = tprovide(Tuple1(value))

  def tprovide[L: Tuple](values: L): RuleDirective[L] =
    RuleDirective { _(values) }

  def extract[T](f: ProxyRequestContext ⇒ T): RuleDirective1[T] =
    textract(ctx ⇒ Tuple1(f(ctx)))

  def textract[L: Tuple](f: ProxyRequestContext ⇒ L): RuleDirective[L] =
    RuleDirective { inner ⇒ ctx ⇒ inner(f(ctx))(ctx) }

  def cancelRejection(rejection: Rejection): RuleDirective0 =
    cancelRejections(_ == rejection)

  def cancelRejections(classes: Class[_]*): RuleDirective0 =
    cancelRejections(r ⇒ classes.exists(_ isInstance r))

  def cancelRejections(cancelFilter: Rejection ⇒ Boolean): RuleDirective0 =
    mapRejections(_ :+ TransformationRejection(_ filterNot cancelFilter))

  def mapUnmatchedPath(f: Uri.Path ⇒ Uri.Path): RuleDirective0 =
    mapProxyRequestContext(_ mapUnmatchedPath f)

  def extractUnmatchedPath: RuleDirective1[Uri.Path] = BasicRuleDirectives._extractUnmatchedPath

  def extractMatchedPath: RuleDirective1[Uri.Path] = BasicRuleDirectives._extractMatchedPath

  def extractRequest: RuleDirective1[HttpRequest] = BasicRuleDirectives._extractRequest

  def extractUri: RuleDirective1[Uri] = BasicRuleDirectives._extractUri

  def withExecutionContext(ec: ExecutionContextExecutor): RuleDirective0 =
    mapProxyRequestContext(_ withExecutionContext ec)

  def extractExecutionContext: RuleDirective1[ExecutionContextExecutor] = BasicRuleDirectives._extractExecutionContext

  def withMaterializer(materializer: Materializer): RuleDirective0 =
    mapProxyRequestContext(_ withMaterializer materializer)

  def extractMaterializer: RuleDirective1[Materializer] = BasicRuleDirectives._extractMaterializer

  def extractActorSystem: RuleDirective1[ActorSystem] = extract { ctx ⇒
    ctx.materializer match {
      case m: ActorMaterializer ⇒ m.system
      case _ ⇒ throw new IllegalArgumentException(s"required [${classOf[ActorMaterializer].getName}] " +
        s"but got [${ctx.materializer.getClass.getName}]")
    }
  }

  def withLog(log: LoggingAdapter): RuleDirective0 =
    mapProxyRequestContext(_ withLog log)

  def extractLog: RuleDirective1[LoggingAdapter] =
    BasicRuleDirectives._extractLog

  def extractProxyRequestContext: RuleDirective1[ProxyRequestContext] = BasicRuleDirectives._extractProxyRequestContext

  def extractRequestEntity: RuleDirective1[RequestEntity] = BasicRuleDirectives._extractRequestEntity

  def extractDataBytes: RuleDirective1[Source[ByteString, Any]] = BasicRuleDirectives._extractDataBytes

}

object BasicRuleDirectives extends BasicRuleDirectives {
  private val _extractUnmatchedPath: RuleDirective1[Uri.Path] = extract(_.unmatchedPath)
  private val _extractMatchedPath: RuleDirective1[Uri.Path] = extract(extractMatched)
  private val _extractRequest: RuleDirective1[HttpRequest] = extract(_.request)
  private val _extractUri: RuleDirective1[Uri] = extract(_.request.uri)
  private val _extractExecutionContext: RuleDirective1[ExecutionContextExecutor] = extract(_.executionContext)
  private val _extractMaterializer: RuleDirective1[Materializer] = extract(_.materializer)
  private val _extractLog: RuleDirective1[LoggingAdapter] = extract(_.log)
  private val _extractProxyRequestContext: RuleDirective1[ProxyRequestContext] = extract(identity)
  private val _extractRequestEntity: RuleDirective1[RequestEntity] = extract(_.request.entity)
  private val _extractDataBytes: RuleDirective1[Source[ByteString, Any]] = extract(_.request.entity.dataBytes)

  private def extractMatched(ctx: ProxyRequestContext) = {
    val unmatchedPath = ctx.unmatchedPath.toString
    val fullPath = ctx.request.uri.path.toString

    require(
      fullPath.endsWith(unmatchedPath),
      s"Unmatched path '$unmatchedPath' wasn't a suffix of full path '$fullPath'. " +
        "This usually means that ctx.unmatchedPath was manipulated inconsistently " +
        "with ctx.request.uri.path"
    )

    Path(fullPath.substring(0, fullPath.length - unmatchedPath.length))
  }
}

