/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 * 2017- Modified by Yusuke Yasuda
 */

package com.github.tanukkii007.migrationproxy.core.rules

import akka.http.scaladsl.util.FastFuture
import akka.http.scaladsl.util.FastFuture._
import RuleDirectives._

/**
  * Ported from akka.http.scaladsl.server.RouteConcatenation
  */
trait RuleConcatenation {

  implicit def _enhanceRuleWithConcatenation(route: Rule): RuleConcatenation.RuleWithConcatenation =
    new RuleConcatenation.RuleWithConcatenation(route: Rule)

  def concat(routes: Rule*): Rule = routes.foldLeft[Rule](reject)(_ ~ _)
}

object RuleConcatenation extends RuleConcatenation {

  class RuleWithConcatenation(route: Rule) {

    def ~(other: Rule): Rule = { ctx ⇒
      import ctx.executionContext
      route(ctx).fast.flatMap {
        case x: RuleResult.Passed ⇒ FastFuture.successful(x)
        case RuleResult.Rejected(outerRejections) ⇒
          other(ctx).fast.map {
            case x: RuleResult.Passed               ⇒ x
            case RuleResult.Rejected(innerRejections) ⇒ RuleResult.Rejected(outerRejections ++ innerRejections)
            case x: RuleResult.Ignored => x
            case x: RuleResult.Failed => x
          }
        case x: RuleResult.Ignored => FastFuture.successful(x)
        case x: RuleResult.Failed => FastFuture.successful(x)
      }
    }
  }
}
