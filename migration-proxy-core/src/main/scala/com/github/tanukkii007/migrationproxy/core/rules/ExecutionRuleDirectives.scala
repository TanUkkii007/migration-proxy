/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 * 2017- Modified by Yusuke Yasuda
 */

package com.github.tanukkii007.migrationproxy.core.rules

import scala.concurrent.Future
import scala.util.control.NonFatal
import akka.http.scaladsl.util.FastFuture._
import com.github.tanukkii007.migrationproxy.core.server.ProxyExceptionHandler

/**
  * Ported from akka.http.scaladsl.server.directives.ExecutionDirectives
  */
trait ExecutionRuleDirectives {
  import BasicRuleDirectives._

  def handleExceptions(handler: ProxyExceptionHandler): RuleDirective0 =
    RuleDirective { innerRuleBuilder ⇒ ctx ⇒
      import ctx.executionContext
      def handleException: PartialFunction[Throwable, Future[RuleResult]] =
        handler andThen (_(ctx))
      try innerRuleBuilder(())(ctx).fast.recoverWith(handleException)
      catch {
        case NonFatal(e) ⇒ handleException.applyOrElse[Throwable, Future[RuleResult]](e, throw _)
      }
    }
}

object ExecutionRuleDirectives extends ExecutionRuleDirectives

