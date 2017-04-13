package com.github.tanukkii007.migrationproxy.core.rules

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Rejection

import scala.collection.immutable

sealed trait RuleResult

object RuleResult {
  final case class Ignored(request: HttpRequest) extends RuleResult
  final case class Passed(request: HttpRequest, response: HttpResponse) extends RuleResult
  final case class Rejected(rejections: immutable.Seq[Rejection]) extends RuleResult
  final case class Failed(request: HttpRequest, response: HttpResponse, cause: Throwable) extends RuleResult
}
