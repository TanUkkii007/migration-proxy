package com.github.tanukkii007.migrationproxy.core.reporter

import akka.actor.{Actor, Props}
import akka.event.Logging
import com.github.tanukkii007.migrationproxy.core.check.CheckResultAggregator


class DefaultReporter extends Actor {

  var totalFailed = 0
  var totalRejected = 0
  var totalIgnored = 0
  var totalPassed = 0

  val successTag = s"${Console.BLACK}[${Console.GREEN}passed${Console.BLACK}]"
  val failedTag = s"${Console.BLACK}[${Console.RED}failed${Console.BLACK}]"
  val rejectedTag = s"${Console.BLACK}[${Console.YELLOW}rejected${Console.BLACK}]"
  val ignoredTag = s"${Console.BLACK}[ignored]"

  context.system.eventStream.subscribe(self, classOf[CheckResultAggregator.ReportEvent])

  def receive: Receive = {
    case CheckResultAggregator.ReportFailed(result) =>
      totalFailed += 1
      val s = s"Check failed in request: ${result.request} for"
      val e = s"Expected response: ${result.response}"
      println(failedTag + " " + Console.RED + List(s, Logging.stackTraceFor(result.cause), e, Console.BLACK).mkString(""))
    case CheckResultAggregator.ReportRejected(result) =>
      totalRejected += 1
      println(rejectedTag + " " + result.rejections)
    case CheckResultAggregator.ReportIgnored(result) =>
      totalIgnored += 1
      println(ignoredTag + " " + result.request)
    case CheckResultAggregator.ReportPassed(result) =>
      totalPassed += 1
      println(successTag + " " + result.request)
  }

  def totalRequests: Int = totalFailed + totalIgnored + totalPassed

}

object DefaultReporter {
  def props = Props(new DefaultReporter)
}