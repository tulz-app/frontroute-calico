package io.frontroute

import app.tulz.tuplez.ApplyConverter
import app.tulz.tuplez.ApplyConverters
import calico.html.Modifier
import io.frontroute.internal.LocationState
import io.frontroute.internal.UrlString
import io.frontroute.ops.DirectiveOfOptionOps
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

import scala.util.chaining._
import scala.scalajs.js

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

implicit def addNullaryDirectiveApply(directive: Directive0): Route => Route = { subRoute => (location, previous, state) =>
  directive.tapply(_ => subRoute)(location, previous, state)
}

private def complete(result: Resource[IO, HtmlElement[IO]]): Route = (location, _, state) => RouteResult.Matched(state, location, state.consumed, result).pure[IO]

def runEffect(effect: IO[Unit]): Route = (location, _, state) =>
  RouteResult.RunEffect(
    state,
    location,
    List.empty,
    effect
  ).pure[IO]

implicit def elementToRoute(e: => Resource[IO, HtmlElement[IO]]): Route = complete(e)

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

private[frontroute] object Frontroute:

  private given Serializer[IO, js.Any] = new Serializer[IO, js.Any]:
    override def serialize(a: js.Any): IO[js.Any] = IO.pure(a)

    override def deserialize(serialized: js.Any): IO[js.Any] = IO.pure(serialized)

  val history: History[IO, js.Any] = fs2.dom.History[IO, js.Any]
