package com.github.tanukkii007.migrationproxy.core.rules

import akka.http.scaladsl.server.PathMatcher.{Matched, Unmatched}

import akka.http.scaladsl.server._
import PathMatcher._

trait PathRuleDirectives extends PathMatchers with ImplicitPathMatcherConstruction {
  import RuleDirectives._
  import BasicRuleDirectives._

  /**
    * Applies the given [[PathMatcher]] to the remaining unmatched path after consuming a leading slash.
    * The matcher has to match the remaining path completely.
    * If matched the value extracted by the [[PathMatcher]] is extracted on the directive level.
    *
    * @group path
    */
  def path[L](pm: PathMatcher[L]): RuleDirective[L] = pathPrefix(pm ~ PathEnd)

  /**
    * Applies the given [[PathMatcher]] to a prefix of the remaining unmatched path after consuming a leading slash.
    * The matcher has to match a prefix of the remaining path.
    * If matched the value extracted by the PathMatcher is extracted on the directive level.
    *
    * @group path
    */
  def pathPrefix[L](pm: PathMatcher[L]): RuleDirective[L] = rawPathPrefix(Slash ~ pm)

  /**
    * Applies the given matcher directly to a prefix of the unmatched path of the
    * [[RequestContext]] (i.e. without implicitly consuming a leading slash).
    * The matcher has to match a prefix of the remaining path.
    * If matched the value extracted by the PathMatcher is extracted on the directive level.
    *
    * @group path
    */
  def rawPathPrefix[L](pm: PathMatcher[L]): RuleDirective[L] = {
    implicit val LIsTuple = pm.ev
    extract(ctx ⇒ pm(ctx.unmatchedPath)).flatMap {
      case Matched(rest, values) ⇒ tprovide(values) & mapProxyRequestContext(_ withUnmatchedPath rest)
      case Unmatched             ⇒ reject
    }
  }

  /**
    * Checks whether the unmatchedPath of the [[RequestContext]] has a prefix matched by the
    * given PathMatcher. In analogy to the `pathPrefix` directive a leading slash is implied.
    *
    * @group path
    */
  def pathPrefixTest[L](pm: PathMatcher[L]): RuleDirective[L] = rawPathPrefixTest(Slash ~ pm)

  /**
    * Checks whether the unmatchedPath of the [[RequestContext]] has a prefix matched by the
    * given PathMatcher. However, as opposed to the `pathPrefix` directive the matched path is not
    * actually "consumed".
    *
    * @group path
    */
  def rawPathPrefixTest[L](pm: PathMatcher[L]): RuleDirective[L] = {
    implicit val LIsTuple = pm.ev
    extract(ctx ⇒ pm(ctx.unmatchedPath)).flatMap {
      case Matched(_, values) ⇒ tprovide(values)
      case Unmatched          ⇒ reject
    }
  }

  /**
    * Applies the given [[PathMatcher]] to a suffix of the remaining unmatchedPath of the [[RequestContext]].
    * If matched the value extracted by the [[PathMatcher]] is extracted and the matched parts of the path are consumed.
    * Note that, for efficiency reasons, the given [[PathMatcher]] must match the desired suffix in reversed-segment
    * order, i.e. `pathSuffix("baz" / "bar")` would match `/foo/bar/baz`!
    *
    * @group path
    */
  def pathSuffix[L](pm: PathMatcher[L]): RuleDirective[L] = {
    implicit val LIsTuple = pm.ev
    extract(ctx ⇒ pm(ctx.unmatchedPath.reverse)).flatMap {
      case Matched(rest, values) ⇒ tprovide(values) & mapProxyRequestContext(_.withUnmatchedPath(rest.reverse))
      case Unmatched             ⇒ reject
    }
  }

  /**
    * Checks whether the unmatchedPath of the [[RequestContext]] has a suffix matched by the
    * given PathMatcher. However, as opposed to the pathSuffix directive the matched path is not
    * actually "consumed".
    * Note that, for efficiency reasons, the given PathMatcher must match the desired suffix in reversed-segment
    * order, i.e. `pathSuffixTest("baz" / "bar")` would match `/foo/bar/baz`!
    *
    * @group path
    */
  def pathSuffixTest[L](pm: PathMatcher[L]): RuleDirective[L] = {
    implicit val LIsTuple = pm.ev
    extract(ctx ⇒ pm(ctx.unmatchedPath.reverse)).flatMap {
      case Matched(_, values) ⇒ tprovide(values)
      case Unmatched          ⇒ reject
    }
  }

  /**
    * Rejects the request if the unmatchedPath of the [[RequestContext]] is non-empty,
    * or said differently: only passes on the request to its inner route if the request path
    * has been matched completely.
    *
    * @group path
    */
  def pathEnd: RuleDirective0 = rawPathPrefix(PathEnd)

  /**
    * Only passes on the request to its inner route if the request path has been matched
    * completely or only consists of exactly one remaining slash.
    *
    * Note that trailing slash and non-trailing slash URLs are '''not''' the same, although they often serve
    * the same content. It is recommended to serve only one URL version and make the other redirect to it using
    * [[redirectToTrailingSlashIfMissing]] or [[redirectToNoTrailingSlashIfPresent]] directive.
    *
    * For example:
    * {{{
    * def route = {
    *   // redirect '/users/' to '/users', '/users/:userId/' to '/users/:userId'
    *   redirectToNoTrailingSlashIfPresent(Found) {
    *     pathPrefix("users") {
    *       pathEnd {
    *         // user list ...
    *       } ~
    *       path(UUID) { userId =>
    *         // user profile ...
    *       }
    *     }
    *   }
    * }
    * }}}
    *
    * For further information, refer to:
    * @see [[http://googlewebmastercentral.blogspot.de/2010/04/to-slash-or-not-to-slash.html]]
    *
    * @group path
    */
  def pathEndOrSingleSlash: RuleDirective0 = rawPathPrefix(Slash.? ~ PathEnd)

  /**
    * Only passes on the request to its inner route if the request path
    * consists of exactly one remaining slash.
    *
    * @group path
    */
  def pathSingleSlash: RuleDirective0 = pathPrefix(PathEnd)

}
