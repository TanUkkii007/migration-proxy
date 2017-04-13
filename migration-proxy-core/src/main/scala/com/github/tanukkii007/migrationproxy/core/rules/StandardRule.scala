package com.github.tanukkii007.migrationproxy.core.rules

import akka.http.scaladsl.server.util.Tuple
import com.github.tanukkii007.migrationproxy.core.server.ProxyRequestContext

/**
  * Ported from akka.http.scaladsl.server.StandardRoute
  */
abstract class StandardRule extends Rule {
  def toRuleDirective[L: Tuple]: RuleDirective[L] = StandardRule.toRuleDirective(this)
}

object StandardRule {
  def apply(route: Rule): StandardRule = route match {
    case x: StandardRule ⇒ x
    case x                ⇒ new StandardRule { def apply(ctx: ProxyRequestContext) = x(ctx) }
  }

  implicit def toRuleDirective[L: Tuple](route: StandardRule): RuleDirective[L] =
    RuleDirective[L] { _ ⇒ route }
}
