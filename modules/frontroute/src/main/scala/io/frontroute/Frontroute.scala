package io.frontroute

import app.tulz.tuplez.ApplyConverter
import app.tulz.tuplez.ApplyConverters
import calico.html.Modifier
import io.frontroute.internal.LocationState
import io.frontroute.internal.UrlString
import io.frontroute.ops.DirectiveOfOptionOps
import io.frontroute.internal.PathMatchResult
import org.scalajs.dom
import org.scalajs.dom.HTMLAnchorElement
import org.scalajs.dom.MutationObserver
import org.scalajs.dom.MutationObserverInit
import org.scalajs.dom.MutationRecord
import org.scalajs.dom.html
import calico.*
import calico.html.io.given
import calico.html.io.*
import fs2.dom.*
import calico.syntax.*
import cats.effect.*
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.*
import fs2.concurrent.*

import scala.annotation.targetName
import scala.util.chaining.*
import scala.scalajs.js
import scala.util.matching.Regex
import scala.util.matching.Regex.Match
import scala.util.NotGiven

type PathMatcher0 = PathMatcher[Unit]

type Directive0 = Directive[Unit]

def locationProvider(lp: LocationProvider): SetLocationProvider = SetLocationProvider(lp)

implicit def directiveOfOptionSyntax[L](underlying: Directive[Option[L]]): DirectiveOfOptionOps[L] = new DirectiveOfOptionOps(underlying)

private[frontroute] val rejected: IO[RouteResult] = IO.pure(RouteResult.Rejected)

val reject: Route = (_, _, _) => rejected

def debug(message: Any, optionalParams: Any*)(subRoute: Route): Route = { (location, previous, state) =>
  dom.console.debug(message, optionalParams: _*)
  subRoute(location, previous, state)
}

def firstMatch(routes: Route*): Route = (location, previous, state) => {

  def findFirst(rs: List[(Route, Int)]): IO[RouteResult] =
    rs match {
      case Nil                    => rejected
      case (route, index) :: tail =>
        route(location, previous, state.enterConcat(index)).flatMap {
          case RouteResult.Matched(state, location, consumed, result) =>
            RouteResult.Matched(state, location, consumed, result).pure[IO]
          case RouteResult.RunEffect(state, location, consumed, run)  =>
            RouteResult.RunEffect(state, location, consumed, run).pure[IO]
          case RouteResult.Rejected                                   =>
            findFirst(tail)
        }
    }

  findFirst(routes.zipWithIndex.toList)
}

// trait =!:=[A, B] {
//   def impossible(proof: A =:= B): Nothing
// }

// sealed trait InvT[X, Y]

// implicit def thingsNotUnit[T]: T =!:= Unit = new =!:=[T, Unit] {
//   type Map[X, Y] = InvT[X, Y] match {
//     case InvT[Unit, T] => Unit
//     case InvT[T, Unit] => Nothing
//   }

//   override def impossible(proof: T =:= Unit): Nothing =
//     proof.substituteBoth[Map](())
// }

extension (directive: Directive0)
  // @targetName("apply_node")
  // def apply[N <: fs2.dom.Node[IO]](subRoute: => Resource[IO, N]): Route =
  // (location, previous, state) => directive.tapply(_ => complete(subRoute))(location, previous, state)

  def apply(subRoute: Route): Route =
    (location, previous, state) => directive.tapply(_ => subRoute)(location, previous, state)

extension [L](directive: Directive[L]) // (using NotGiven[L =:= Unit])

  // @targetName("apply_node")
  // def apply[N <: fs2.dom.Node[IO]](subRoute: L => Resource[IO, N]): Route =
  // (location, previous, state) => directive.tapply(l => complete(subRoute(l)))(location, previous, state)

  def apply(subRoute: L => Route): Route =
    (location, previous, state) => directive.tapply(subRoute)(location, previous, state)

// implicit def addNullaryDirectiveApplyWithElement[N <: fs2.dom.Node[IO]](directive: Directive0): Resource[IO, N] => Route = { (subRoute: Resource[IO, N]) => (location, previous, state) =>
// directive.tapply(_ => complete(subRoute))(location, previous, state)
// }

// implicit def addDirectiveApply[L](directive: Directive[L]): (L => Route) => Route = { subRoute => (location, previous, state) =>
//   directive.tapply(subRoute)(location, previous, state)
// }

private def complete[N <: fs2.dom.Node[IO]](result: Resource[IO, N]): Route = (location, _, state) => RouteResult.Matched(state, location, state.consumed, result).pure[IO]

def runEffect(effect: IO[Unit]): Route = (location, _, state) =>
  RouteResult
    .RunEffect(
      state,
      location,
      List.empty,
      effect
    ).pure[IO]

implicit def nodeToRoute[N <: fs2.dom.Node[IO]](e: => Resource[IO, N]): Route = complete(e)

private def makeRelative(matched: List[String], path: String): String =
  if (matched.nonEmpty) {
    if (path.nonEmpty) {
      matched.mkString("/", "/", s"/$path")
    } else {
      matched.mkString("/", "/", "")
    }
  } else {
    if (path.nonEmpty) {
      s"/$path"
    } else {
      "/"
    }
  }

//def navigate(
//  to: String,
//  replace: Boolean = false,
//): Route =
//  extractMatchedPath { matched =>
//    runEffect {
//      if (replace) {
//        BrowserNavigation.replaceState(url = makeRelative(matched, to))
//      } else {
//        BrowserNavigation.pushState(url = makeRelative(matched, to))
//      }
//    }
//  }

//def withMatchedPath[Ref <: dom.html.Element](mod: StrictSignal[List[String]] => Mod[ReactiveHtmlElement[Ref]]): Mod[ReactiveHtmlElement[Ref]] = {
//  val consumedVar = Var(List.empty[String])
//  var sub: js.UndefOr[DynamicSubscription] = js.undefined
//
//  Seq(
//    onMountCallback { (ctx: MountContext[ReactiveHtmlElement[Ref]]) =>
//      LocationState.closest(ctx.thisNode.ref) match {
//        case None =>
//          sub = ReactiveElement.bindFn(ctx.thisNode, LocationState.default.consumed) { next =>
//            LocationState.closest(ctx.thisNode.ref) match {
//              case None => consumedVar.set(next)
//              case Some(locationState) =>
//                sub.foreach(_.kill())
//                sub = js.undefined
//                // managed subscription
//                val _ = ReactiveElement.bindObserver(ctx.thisNode, locationState.consumed)(consumedVar.writer)
//            }
//          }
//        case Some(locationState) =>
//          // managed subscription
//          val _ = ReactiveElement.bindObserver(ctx.thisNode, locationState.consumed)(consumedVar.writer)
//      }
//    },
//    mod(consumedVar.signal)
//  )
//}
//
//def relativeHref(path: String): Mod[ReactiveHtmlElement[html.Anchor]] =
//  withMatchedPath { matched =>
//    href <-- matched.map { matched =>
//      makeRelative(matched, path)
//    }
//  }

case class NavMod[M](
  compare: (io.frontroute.Location, org.scalajs.dom.Location) => Boolean,
  mod: Signal[IO, Boolean] => M
)

object NavMod:
  given [M](using M: Modifier[IO, fs2.dom.HtmlAnchorElement[IO], M]): Modifier[IO, fs2.dom.HtmlAnchorElement[IO], NavMod[M]] =
    (m, e) => {
      Resource
        .eval(
          (
            SignallingRef.of[IO, Boolean](false),
            fs2.concurrent.Channel.bounded[IO, Seq[MutationRecord]](5)
          ).tupled
        )
        .flatMap { case (activeVar, mutations) =>
          Resource
            .make[IO, MutationObserver](
              IO.delay {
                new MutationObserver(
                  callback = (entries, _) => {
                    if (entries.nonEmpty) {
                      mutations.send(entries.toSeq)
                    }
                  }
                ).tap {
                  _.observe(
                    e.asInstanceOf,
                    new MutationObserverInit {
                      attributes = true
                      attributeFilter = js.Array("href")
                    }
                  )
                }
              }
            )(obs => IO.delay { obs.disconnect() }).flatMap { _ =>
              LocationState.closestOrDefault(e).flatMap { locationState =>
                fs2.Stream
                  .emit[IO, Unit](()).merge(mutations.stream.void)
                  .evalMap(_ => locationState.location.get)
                  .merge(locationState.location.discrete)
                  .map { location =>
                    val UrlString(url) = e.asInstanceOf[dom.HTMLAnchorElement].href
                    location.exists { location =>
                      m.compare(location, url)
                    }
                  }
                  .holdResource(false)
                  .flatMap { active =>
                    M.modify(m.mod(active), e)
                  }
              }
            }
        }
    }

def navModFn[M](
  compare: (io.frontroute.Location, org.scalajs.dom.Location) => Boolean
)(
  mod: Signal[IO, Boolean] => M
): NavMod[M] = NavMod(compare, mod)

def navMod[M](mod: Signal[IO, Boolean] => M): NavMod[M] = NavMod(
  (location, url) => location.fullPath.mkString("/", "/", "").startsWith(url.pathname),
  mod
)

def navModExact[M](mod: Signal[IO, Boolean] => M): NavMod[M] = NavMod(
  (location, url) => location.fullPath.mkString("/", "/", "") == url.pathname,
  mod
)

// --

// private[frontroute] def withMatchedPathAndEl[N <: fs2.dom.Node[IO], O](
//   body: (N, Signal[IO, List[String]]) => O
// ): O = {
//   SignallingRef.of[IO, List[String]](List.empty).map { consumedVar =>
//     LocationState.closest(el.ref) match {
//       case None                =>
//         sub = ReactiveElement.bindFn(el, LocationState.default.consumed) { next =>
//           LocationState.closest(el.ref) match {
//             case None                => consumedVar.set(next)
//             case Some(locationState) =>
//               sub.foreach(_.kill())
//               sub = js.undefined
//               // managed subscription
//               val _ = ReactiveElement.bindObserver(el, locationState.consumed)(consumedVar.writer)
//           }
//         }
//       case Some(locationState) =>
//         // managed subscription
//         val _ = ReactiveElement.bindObserver(el, locationState.consumed)(consumedVar.writer)
//     }
//     body(el, consumedVar.signal)
//   }
// }

// def withMatchedPath[Ref <: dom.html.Element](mod: StrictSignal[List[String]] => Mod[ReactiveHtmlElement[Ref]]): Mod[ReactiveHtmlElement[Ref]] = {
//   withMatchedPathAndEl((_, consumed) => mod(consumed))
// }

// def relativeHref(path: String): Mod[ReactiveHtmlElement[html.Anchor]] =
//   withMatchedPath { matched =>
//     href <-- matched.map(_.mkString("/", "/", s"/$path"))
//   }

private[frontroute] def extractContext: Directive[io.frontroute.Location] =
  Directive[io.frontroute.Location](inner => (location, previous, state) => inner(location)(location, previous, state))

private[frontroute] def extract[T](f: io.frontroute.Location => T): Directive[T] =
  extractContext.map(f)

def param(name: String): Directive[String] =
  Directive[String] { inner => (location, previous, state) =>
    location.params.get(name).flatMap(_.headOption) match {
      case Some(paramValue) => inner(paramValue)(location, previous, state.enterAndSet(paramValue))
      case None             => rejected
    }
  }

def historyState: Directive[Option[js.Any]] =
  extractContext.map(_.parsedState.flatMap(_.user.toOption))

def historyScroll: Directive[Option[ScrollPosition]] =
  extractContext.map(_.parsedState.flatMap(_.internal.toOption).flatMap(_.scroll.toOption).map { scroll =>
    ScrollPosition(
      scrollX = scroll.scrollX.toOption.map(_.round.toInt),
      scrollY = scroll.scrollY.toOption.map(_.round.toInt)
    )
  })

def maybeParam(name: String): Directive[Option[String]] =
  Directive[Option[String]] { inner => (location, previous, state) =>
    val maybeParamValue = location.params.get(name).flatMap(_.headOption)
    inner(maybeParamValue)(location, previous, state.enterAndSet(maybeParamValue))
  }

def extractMatchedPath: Directive[List[String]] =
  Directive[List[String]](inner => (location, previous, state) => inner(state.consumed)(location, previous, state))

val extractUnmatchedPath: Directive[List[String]] = extract(_.path)

val extractHostname: Directive[String] = extract(_.hostname)

val extractPort: Directive[String] = extract(_.port)

val extractHost: Directive[String] = extract(_.host)

val extractProtocol: Directive[String] = extract(_.protocol)

val extractOrigin: Directive[Option[String]] = extract(_.origin)

def provide[L](value: L): Directive[L] = Directive.provide(value)

def provideOption[L](value: Option[L]): Directive[L] =
  Directive { inner => (location, previous, state) =>
    value match {
      case None        => rejected
      case Some(value) => inner(value)(location, previous, state.enterAndSet(value))
    }
  }

def pathPrefix[T](m: PathMatcher[T]): Directive[T] =
  Directive[T] { inner => (location, previous, state) =>
    m(state.consumed, location.path) match {
      case PathMatchResult.Match(t, consumed, rest) =>
        inner(t)(location.withUnmatchedPath(rest), previous, state.enterAndSet(t).withConsumed(consumed))
      case _                                        => rejected
    }
  }

def testPathPrefix[T](m: PathMatcher[T]): Directive[T] =
  Directive[T] { inner => (location, previous, state) =>
    m(state.consumed, location.path) match {
      case PathMatchResult.Match(t, _, _) => inner(t)(location, previous, state.enterAndSet(t))
      case _                              => rejected
    }
  }

val pathEnd: Directive0 =
  Directive[Unit] { inner => (location, previous, state) =>
    if (location.path.isEmpty) {
      inner(())(location, previous, state.enter)
    } else {
      rejected
    }
  }

def path[T](m: PathMatcher[T]): Directive[T] =
  Directive[T] { inner => (location, previous, state) =>
    m(state.consumed, location.path) match {
      case PathMatchResult.Match(t, consumed, Nil) =>
        inner(t)(location.withUnmatchedPath(List.empty), previous, state.enterAndSet(t).withConsumed(consumed))
      case _                                       => rejected
    }
  }

def testPath[T](m: PathMatcher[T]): Directive[T] =
  Directive[T] { inner => (location, previous, state) =>
    m(state.consumed, location.path) match {
      case PathMatchResult.Match(t, _, Nil) => inner(t)(location, previous, state.enterAndSet(t))
      case _                                => rejected
    }
  }

val noneMatched: Directive0 =
  Directive[Unit] { inner => (location, previous, state) =>
    println(s"location.otherMatched: ${location.otherMatched} ($location) (${state})")
    if (location.otherMatched) {
      rejected
    } else {
      inner(())(location, previous, state.enter)
    }
  }

def whenTrue(condition: => Boolean): Directive0 =
  Directive[Unit] { inner => (location, previous, state) =>
    if (condition) {
      inner(())(location, previous, state)
    } else {
      rejected
    }
  }

@inline def whenFalse(condition: => Boolean): Directive0 = whenTrue(!condition)

def segment: PathMatcher[String] =
  (consumed: List[String], in: List[String]) =>
    in match {
      case head :: tail => PathMatchResult.Match(head, consumed.appended(head), tail)
      case Nil          => PathMatchResult.NoMatch
    }

def segment(oneOf: Seq[String]): PathMatcher[String] =
  (consumed: List[String], in: List[String]) =>
    in match {
      case head :: tail =>
        if (oneOf.contains(head)) {
          PathMatchResult.Match(head, consumed.appended(head), tail)
        } else {
          PathMatchResult.Rejected(tail)
        }
      case Nil          => PathMatchResult.NoMatch
    }

def segment(oneOf: Set[String]): PathMatcher[String] =
  (consumed: List[String], in: List[String]) =>
    in match {
      case head :: tail =>
        if (oneOf.contains(head)) {
          PathMatchResult.Match(head, consumed.appended(head), tail)
        } else {
          PathMatchResult.Rejected(tail)
        }
      case Nil          => PathMatchResult.NoMatch
    }

def segment(s: String): PathMatcher0 =
  (consumed: List[String], in: List[String]) =>
    in match {
      case head :: tail =>
        if (head == s) {
          PathMatchResult.Match((), consumed.appended(head), tail)
        } else {
          PathMatchResult.Rejected(tail)
        }
      case Nil          => PathMatchResult.NoMatch
    }

def regex(r: Regex): PathMatcher[Match] =
  segment
    .map { s => r.findFirstMatchIn(s) }
    .collect { case Some(m) => m }

def long: PathMatcher[Long] = segment.tryParse(_.toLong)

def double: PathMatcher[Double] = segment.tryParse(_.toDouble)

/*private[frontroute] */
object Frontroute:

  val history: History[IO, Unit] = Window[IO].history[Unit]
