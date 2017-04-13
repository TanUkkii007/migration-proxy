/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 * 2017- Modified by Yusuke Yasuda
 */

package com.github.tanukkii007.migrationproxy.core.rules

import akka.http.scaladsl.server.util.Tupler
import akka.http.scaladsl.util.FastFuture._
import scala.concurrent.Future
import scala.util.Try

/**
  * Ported from akka.http.scaladsl.server.directives.FutureDirectives
  */
trait FutureRuleDirectives {

  import RuleDirectives._

  def onComplete[T](future: ⇒ Future[T]): RuleDirective1[Try[T]] =
    RuleDirective { inner ⇒ ctx ⇒
      import ctx.executionContext
      future.fast.transformWith(t ⇒ inner(Tuple1(t))(ctx))
    }

  def onSuccess(magnet: OnSuccessMagnet): RuleDirective[magnet.Out] = magnet.directive

}

object FutureRuleDirectives extends FutureRuleDirectives

trait OnSuccessMagnet {
  type Out
  def directive: RuleDirective[Out]
}

object OnSuccessMagnet {
  implicit def apply[T](future: ⇒ Future[T])(implicit tupler: Tupler[T]): OnSuccessMagnet { type Out = tupler.Out } =
    new OnSuccessMagnet {
      type Out = tupler.Out
      val directive = RuleDirective[tupler.Out] { inner ⇒ ctx ⇒
        import ctx.executionContext
        future.fast.flatMap(t ⇒ inner(tupler(t))(ctx))
      }(tupler.OutIsTuple)
    }
}
