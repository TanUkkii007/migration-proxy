/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 * 2017- Modified by Yusuke Yasuda
 */

package com.github.tanukkii007.migrationproxy.core.rules

import akka.http.scaladsl.common._
import akka.http.scaladsl.server.{MalformedQueryParamRejection, MissingQueryParamRejection}

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Ported from akka.http.scaladsl.server.directives.ParameterDirectives
  */
trait ParameterRuleDirectives {
  import ParameterRuleDirectives._

  def parameterMap: RuleDirective1[Map[String, String]] = _parameterMap

  def parameterMultiMap: RuleDirective1[Map[String, List[String]]] = _parameterMultiMap

  def parameterSeq: RuleDirective1[immutable.Seq[(String, String)]] = _parameterSeq

  def parameter(pdm: ParamMagnet): pdm.Out = pdm()

  def parameters(pdm: ParamMagnet): pdm.Out = pdm()

  // ToNameReceptacleEnhancements
  implicit def _symbol2NR(symbol: Symbol): NameReceptacle[String] = new NameReceptacle[String](symbol.name)
  implicit def _string2NR(string: String): NameReceptacle[String] = new NameReceptacle[String](string)

}

object ParameterRuleDirectives extends ParameterRuleDirectives {
  import BasicRuleDirectives._

  private val _parameterMap: RuleDirective1[Map[String, String]] =
    extract(_.request.uri.query().toMap)

  private val _parameterMultiMap: RuleDirective1[Map[String, List[String]]] =
    extract(_.request.uri.query().toMultiMap)

  private val _parameterSeq: RuleDirective1[immutable.Seq[(String, String)]] =
    extract(_.request.uri.query().toSeq)

  sealed trait ParamMagnet {
    type Out
    def apply(): Out
  }
  object ParamMagnet {
    implicit def apply[T](value: T)(implicit pdef: ParamDef[T]): ParamMagnet { type Out = pdef.Out } =
      new ParamMagnet {
        type Out = pdef.Out
        def apply() = pdef(value)
      }
  }

  type ParamDefAux[T, U] = ParamDef[T] { type Out = U }
  sealed trait ParamDef[T] {
    type Out
    def apply(value: T): Out
  }
  object ParamDef {
    def paramDef[A, B](f: A ⇒ B): ParamDefAux[A, B] =
      new ParamDef[A] {
        type Out = B
        def apply(value: A) = f(value)
      }

    import akka.http.scaladsl.unmarshalling.{ FromStringUnmarshaller ⇒ FSU, _ }
    import BasicRuleDirectives._
    import RuleDirectives._
    import FutureRuleDirectives._
    type FSOU[T] = Unmarshaller[Option[String], T]

    private def extractParameter[A, B](f: A ⇒ RuleDirective1[B]): ParamDefAux[A, RuleDirective1[B]] = paramDef(f)
    private def handleParamResult[T](paramName: String, result: Future[T])(implicit ec: ExecutionContext): RuleDirective1[T] =
      onComplete(result).flatMap {
        case Success(x)                               ⇒ provide(x)
        case Failure(Unmarshaller.NoContentException) ⇒ reject(MissingQueryParamRejection(paramName))
        case Failure(x)                               ⇒ reject(MalformedQueryParamRejection(paramName, Option(x.getMessage).getOrElse(""), Option(x.getCause)))
      }

    //////////////////// "regular" parameter extraction //////////////////////

    private def filter[T](paramName: String, fsou: FSOU[T]): RuleDirective1[T] =
      extractProxyRequestContext flatMap { ctx ⇒
        import ctx.executionContext
        import ctx.materializer
        handleParamResult(paramName, fsou(ctx.request.uri.query().get(paramName)))
      }
    implicit def forString(implicit fsu: FSU[String]): ParamDefAux[String, RuleDirective1[String]] =
      extractParameter[String, String] { string ⇒ filter(string, fsu) }
    implicit def forSymbol(implicit fsu: FSU[String]): ParamDefAux[Symbol, RuleDirective1[String]] =
      extractParameter[Symbol, String] { symbol ⇒ filter(symbol.name, fsu) }
    implicit def forNR[T](implicit fsu: FSU[T]): ParamDefAux[NameReceptacle[T], RuleDirective1[T]] =
      extractParameter[NameReceptacle[T], T] { nr ⇒ filter(nr.name, fsu) }
    implicit def forNUR[T]: ParamDefAux[NameUnmarshallerReceptacle[T], RuleDirective1[T]] =
      extractParameter[NameUnmarshallerReceptacle[T], T] { nr ⇒ filter(nr.name, nr.um) }
    implicit def forNOR[T](implicit fsou: FSOU[T]): ParamDefAux[NameOptionReceptacle[T], RuleDirective1[Option[T]]] =
      extractParameter[NameOptionReceptacle[T], Option[T]] { nr ⇒ filter[Option[T]](nr.name, fsou) }
    implicit def forNDR[T](implicit fsou: FSOU[T]): ParamDefAux[NameDefaultReceptacle[T], RuleDirective1[T]] =
      extractParameter[NameDefaultReceptacle[T], T] { nr ⇒ filter[T](nr.name, fsou withDefaultValue nr.default) }
    implicit def forNOUR[T]: ParamDefAux[NameOptionUnmarshallerReceptacle[T], RuleDirective1[Option[T]]] =
      extractParameter[NameOptionUnmarshallerReceptacle[T], Option[T]] { nr ⇒ filter(nr.name, nr.um: FSOU[T]) }
    implicit def forNDUR[T]: ParamDefAux[NameDefaultUnmarshallerReceptacle[T], RuleDirective1[T]] =
      extractParameter[NameDefaultUnmarshallerReceptacle[T], T] { nr ⇒ filter[T](nr.name, (nr.um: FSOU[T]) withDefaultValue nr.default) }

    //////////////////// required parameter support ////////////////////

    private def requiredFilter[T](paramName: String, fsou: FSOU[T], requiredValue: Any): RuleDirective0 =
      extractProxyRequestContext flatMap { ctx ⇒
        import ctx.executionContext
        import ctx.materializer
        onComplete(fsou(ctx.request.uri.query().get(paramName))) flatMap {
          case Success(value) if value == requiredValue ⇒ pass
          case _                                        ⇒ reject
        }
      }
    implicit def forRVR[T](implicit fsu: FSU[T]): ParamDefAux[RequiredValueReceptacle[T], RuleDirective0] =
      paramDef[RequiredValueReceptacle[T], RuleDirective0] { rvr ⇒ requiredFilter(rvr.name, fsu, rvr.requiredValue) }
    implicit def forRVDR[T]: ParamDefAux[RequiredValueUnmarshallerReceptacle[T], RuleDirective0] =
      paramDef[RequiredValueUnmarshallerReceptacle[T], RuleDirective0] { rvr ⇒ requiredFilter(rvr.name, rvr.um, rvr.requiredValue) }

    //////////////////// repeated parameter support ////////////////////

    private def repeatedFilter[T](paramName: String, fsu: FSU[T]): RuleDirective1[Iterable[T]] =
      extractProxyRequestContext flatMap { ctx ⇒
        import ctx.executionContext
        import ctx.materializer
        handleParamResult(paramName, Future.sequence(ctx.request.uri.query().getAll(paramName).map(fsu.apply)))
      }
    implicit def forRepVR[T](implicit fsu: FSU[T]): ParamDefAux[RepeatedValueReceptacle[T], RuleDirective1[Iterable[T]]] =
      extractParameter[RepeatedValueReceptacle[T], Iterable[T]] { rvr ⇒ repeatedFilter(rvr.name, fsu) }
    implicit def forRepVDR[T]: ParamDefAux[RepeatedValueUnmarshallerReceptacle[T], RuleDirective1[Iterable[T]]] =
      extractParameter[RepeatedValueUnmarshallerReceptacle[T], Iterable[T]] { rvr ⇒ repeatedFilter(rvr.name, rvr.um) }

    //////////////////// tuple support ////////////////////

    import akka.http.scaladsl.server.util.TupleOps._
    import akka.http.scaladsl.server.util.BinaryPolyFunc

    implicit def forTuple[T](implicit fold: FoldLeft[RuleDirective0, T, ConvertParamDefAndConcatenate.type]): ParamDefAux[T, fold.Out] =
      paramDef[T, fold.Out](fold(BasicRuleDirectives.pass, _))

    object ConvertParamDefAndConcatenate extends BinaryPolyFunc {
      implicit def from[P, TA, TB](implicit pdef: ParamDef[P] { type Out = RuleDirective[TB] }, ev: Join[TA, TB]): BinaryPolyFunc.Case[RuleDirective[TA], P, ConvertParamDefAndConcatenate.type] { type Out = RuleDirective[ev.Out] } =
        at[RuleDirective[TA], P] { (a, t) ⇒ a & pdef(t) }
    }
  }

}

