/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 * 2017- Modified by Yusuke Yasuda
 */

package com.github.tanukkii007.migrationproxy.core

import com.github.tanukkii007.migrationproxy.core.server.ProxyRequestContext

import scala.concurrent.Future

/**
  * Ported from akka.http.scaladsl.server
  */
package object rules {
  type Rule = ProxyRequestContext => Future[RuleResult]
  type RuleDirective0 = RuleDirective[Unit]
  type RuleDirective1[T] = RuleDirective[Tuple1[T]]
}
