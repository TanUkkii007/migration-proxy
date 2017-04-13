/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 * 2017- Modified by Yusuke Yasuda
 */

package com.github.tanukkii007.migrationproxy.core.rules

import akka.http.scaladsl.server.Rejection

/**
  * Ported form akka.http.scaladsl.server.directives.RouteDirectives
  */
trait RuleDirectives {

  def ignore: StandardRule = RuleDirectives._ignore

  def reject: StandardRule = RuleDirectives._reject

  def reject(rejections: Rejection*): StandardRule =
    StandardRule(_.reject(rejections: _*))

  def complete: StandardRule =
    StandardRule(_.complete)

  def failWith(error: Throwable): StandardRule =
    StandardRule(_.fail(error))
}

object RuleDirectives extends RuleDirectives {
  private val _ignore = StandardRule(_.ignore)

  private val _reject = StandardRule(_.reject())
}

