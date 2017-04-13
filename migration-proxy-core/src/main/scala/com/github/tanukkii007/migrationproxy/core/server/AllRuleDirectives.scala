/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 * 2017- Modified by Yusuke Yasuda
 */

package com.github.tanukkii007.migrationproxy.core.server

import com.github.tanukkii007.migrationproxy.core.rules._

/**
  * Ported from akka.http.scaladsl.server.Directives
  */
trait AllRuleDirectives extends RuleDirectives
  with RuleConcatenation
  with BasicRuleDirectives
  with FutureRuleDirectives
  with MethodRuleDirectives
  with ParameterRuleDirectives
  with PathRuleDirectives
  with ExecutionRuleDirectives
  with ForwardRuleDirectives {

}

object AllRuleDirectives extends AllRuleDirectives