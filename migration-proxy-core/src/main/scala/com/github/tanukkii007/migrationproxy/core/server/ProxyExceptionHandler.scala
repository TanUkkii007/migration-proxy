/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 * 2017- Modified by Yusuke Yasuda
 */

package com.github.tanukkii007.migrationproxy.core.server

import scala.util.control.NonFatal
import akka.http.scaladsl.model._
import com.github.tanukkii007.migrationproxy.core.rules.Rule

/**
  * Ported from akka.http.scaladsl.server.ExceptionHandler
  */
trait ProxyExceptionHandler extends ProxyExceptionHandler.PF {

  def withFallback(that: ProxyExceptionHandler): ProxyExceptionHandler

  def seal: ProxyExceptionHandler
}

object ProxyExceptionHandler {
  type PF = PartialFunction[Throwable, Rule]

  implicit def apply(pf: PF): ProxyExceptionHandler = apply(knownToBeSealed = false)(pf)

  private def apply(knownToBeSealed: Boolean)(pf: PF): ProxyExceptionHandler =
    new ProxyExceptionHandler {
      def isDefinedAt(error: Throwable) = pf.isDefinedAt(error)
      def apply(error: Throwable) = pf(error)
      def withFallback(that: ProxyExceptionHandler): ProxyExceptionHandler =
        if (!knownToBeSealed) ProxyExceptionHandler(knownToBeSealed = false)(this orElse that) else this
      def seal: ProxyExceptionHandler =
        if (!knownToBeSealed) ProxyExceptionHandler(knownToBeSealed = true)(this orElse default) else this
    }

  def default: ProxyExceptionHandler =
    apply(knownToBeSealed = true) {
      case NonFatal(e) ⇒ ctx ⇒ {
        val message = Option(e.getMessage).getOrElse(s"${e.getClass.getName} (No error message supplied)")
        ctx.fail(e)
      }
    }

  def seal(handler: Option[ProxyExceptionHandler] = None): ProxyExceptionHandler =
    if (handler.nonEmpty) handler.get.seal else ProxyExceptionHandler.default
}
