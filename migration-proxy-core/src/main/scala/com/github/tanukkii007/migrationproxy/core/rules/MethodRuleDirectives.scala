/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 * 2017- Modified by Yusuke Yasuda
 */

package com.github.tanukkii007.migrationproxy.core.rules

import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server.MethodRejection

/**
  * Ported from akka.http.scaladsl.server.directives.MethodDirectives
  */
trait MethodRuleDirectives {
  import BasicRuleDirectives._
  import RuleDirectives._
  import ParameterRuleDirectives._
  import MethodRuleDirectives._

  def delete: RuleDirective0 = _delete

  def get: RuleDirective0 = _get

  def head: RuleDirective0 = _head

  def options: RuleDirective0 = _options

  def patch: RuleDirective0 = _patch

  def post: RuleDirective0 = _post

  def put: RuleDirective0 = _put

  def extractMethod: RuleDirective1[HttpMethod] = _extractMethod

  def method(httpMethod: HttpMethod): RuleDirective0 =
    extractMethod.flatMap[Unit] {
      case `httpMethod` ⇒ pass
      case _            ⇒ reject(MethodRejection(httpMethod))
    } & cancelRejections(classOf[MethodRejection])

}

object MethodRuleDirectives extends MethodRuleDirectives {
  private val _extractMethod: RuleDirective1[HttpMethod] =
    BasicRuleDirectives.extract(_.request.method)

  // format: OFF
  private val _delete : RuleDirective0 = method(DELETE)
  private val _get    : RuleDirective0 = method(GET)
  private val _head   : RuleDirective0 = method(HEAD)
  private val _options: RuleDirective0 = method(OPTIONS)
  private val _patch  : RuleDirective0 = method(PATCH)
  private val _post   : RuleDirective0 = method(POST)
  private val _put    : RuleDirective0 = method(PUT)
  // format: ON
}

