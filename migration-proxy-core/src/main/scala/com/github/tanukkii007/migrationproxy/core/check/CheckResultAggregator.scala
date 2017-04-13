package com.github.tanukkii007.migrationproxy.core.check

import akka.actor.{Actor, Props}
import com.github.tanukkii007.migrationproxy.core.rules.RuleResult


private class CheckResultAggregator extends Actor {
  import CheckResultAggregator._

  def receive: Receive = {
    case ReportResult(result) =>
      result match {
        case r: RuleResult.Failed =>
          context.system.eventStream.publish(ReportFailed(r))
        case r: RuleResult.Rejected =>
          context.system.eventStream.publish(ReportRejected(r))
        case r: RuleResult.Ignored =>
          context.system.eventStream.publish(ReportIgnored(r))
        case r: RuleResult.Passed =>
          context.system.eventStream.publish(ReportPassed(r))
      }
  }

}

object CheckResultAggregator {
  case class ReportResult(result: RuleResult)

  sealed trait ReportEvent
  case class ReportRejected(result: RuleResult.Rejected) extends ReportEvent
  case class ReportFailed(result: RuleResult.Failed) extends ReportEvent
  case class ReportIgnored(result: RuleResult.Ignored) extends ReportEvent
  case class ReportPassed(result: RuleResult.Passed) extends ReportEvent

  private[core] def props = Props(new CheckResultAggregator)
}