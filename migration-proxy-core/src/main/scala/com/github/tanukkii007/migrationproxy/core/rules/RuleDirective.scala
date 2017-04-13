/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 * 2017- Modified by Yusuke Yasuda
 */

package com.github.tanukkii007.migrationproxy.core.rules

import akka.http.scaladsl.server.{Rejection, RouteResult}
import akka.http.scaladsl.server.util._
import akka.http.scaladsl.util.FastFuture
import akka.http.scaladsl.util.FastFuture._
import scala.collection.immutable

/**
  * Ported from akka.http.scaladsl.server.Directive
  */
abstract class RuleDirective[L](implicit val ev: Tuple[L]) {

  def tapply(f: L ⇒ Rule): Rule

  def |[R >: L](that: RuleDirective[R]): RuleDirective[R] =
    recover(rejections ⇒ BasicRuleDirectives.mapRejections(rejections ++ _) & that)(that.ev)

  def &(magnet: ConjunctionMagnet[L]): magnet.Out = magnet(this)

  def tmap[R](f: L ⇒ R)(implicit tupler: Tupler[R]): RuleDirective[tupler.Out] =
    RuleDirective[tupler.Out] { inner ⇒ tapply { values ⇒ inner(tupler(f(values))) } }(tupler.OutIsTuple)

  def tflatMap[R: Tuple](f: L ⇒ RuleDirective[R]): RuleDirective[R] =
    RuleDirective[R] { inner ⇒ tapply { values ⇒ f(values) tapply inner } }

  def trequire(predicate: L ⇒ Boolean, rejections: Rejection*): RuleDirective0 =
    tfilter(predicate, rejections: _*).tflatMap(_ ⇒ RuleDirective.Empty)

  def tfilter(predicate: L ⇒ Boolean, rejections: Rejection*): RuleDirective[L] =
    RuleDirective[L] { inner ⇒ tapply { values ⇒ ctx ⇒ if (predicate(values)) inner(values)(ctx) else ctx.reject(rejections: _*) } }

  def recover[R >: L: Tuple](recovery: immutable.Seq[Rejection] ⇒ RuleDirective[R]): RuleDirective[R] =
    RuleDirective[R] { inner ⇒ ctx ⇒
      import ctx.executionContext
      @volatile var rejectedFromInnerRoute = false
      tapply({ list ⇒ c ⇒ rejectedFromInnerRoute = true; inner(list)(c) })(ctx).fast.flatMap {
        case RuleResult.Rejected(rejections) if !rejectedFromInnerRoute ⇒ recovery(rejections).tapply(inner)(ctx)
        case x ⇒ FastFuture.successful(x)
      }
    }

  def recoverPF[R >: L: Tuple](recovery: PartialFunction[immutable.Seq[Rejection], RuleDirective[R]]): RuleDirective[R] =
    recover { rejections ⇒ recovery.applyOrElse(rejections, (rejs: Seq[Rejection]) ⇒ RuleDirectives.reject(rejs: _*)) }

}

object RuleDirective {

  def apply[T: Tuple](f: (T ⇒ Rule) ⇒ Rule): RuleDirective[T] =
    new RuleDirective[T] { def tapply(inner: T ⇒ Rule) = f(inner) }

  val Empty: RuleDirective0 = RuleDirective(_(()))

  implicit def addDirectiveApply[L](directive: RuleDirective[L])(implicit hac: ApplyConverter[L]): hac.In ⇒ Rule =
    f ⇒ directive.tapply(hac(f))

  implicit def addByNameNullaryApply(directive: RuleDirective0): (⇒ Rule) ⇒ Rule =
    r ⇒ directive.tapply(_ ⇒ r)

  implicit class SingleValueModifiers[T](underlying: RuleDirective1[T]) extends AnyRef {
    def map[R](f: T ⇒ R)(implicit tupler: Tupler[R]): RuleDirective[tupler.Out] =
      underlying.tmap { case Tuple1(value) ⇒ f(value) }

    def flatMap[R: Tuple](f: T ⇒ RuleDirective[R]): RuleDirective[R] =
      underlying.tflatMap { case Tuple1(value) ⇒ f(value) }

    def require(predicate: T ⇒ Boolean, rejections: Rejection*): RuleDirective0 =
      underlying.filter(predicate, rejections: _*).tflatMap(_ ⇒ Empty)

    def filter(predicate: T ⇒ Boolean, rejections: Rejection*): RuleDirective1[T] =
      underlying.tfilter({ case Tuple1(value) ⇒ predicate(value) }, rejections: _*)
  }
}

trait ConjunctionMagnet[L] {
  type Out
  def apply(underlying: RuleDirective[L]): Out
}

object ConjunctionMagnet {
  implicit def fromDirective[L, R](other: RuleDirective[R])(implicit join: TupleOps.Join[L, R]): ConjunctionMagnet[L] { type Out = RuleDirective[join.Out] } =
    new ConjunctionMagnet[L] {
      type Out = RuleDirective[join.Out]
      def apply(underlying: RuleDirective[L]) =
        RuleDirective[join.Out] { inner ⇒
          underlying.tapply { prefix ⇒ other.tapply { suffix ⇒ inner(join(prefix, suffix)) } }
        }(Tuple.yes) // we know that join will only ever produce tuples
    }

  implicit def fromStandardRoute[L](route: StandardRule): ConjunctionMagnet[L] { type Out = StandardRule } =
    new ConjunctionMagnet[L] {
      type Out = StandardRule
      def apply(underlying: RuleDirective[L]) = StandardRule(underlying.tapply(_ ⇒ route))
    }

}