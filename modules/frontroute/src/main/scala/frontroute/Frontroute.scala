package frontroute

import app.tulz.tuplez.ApplyConverter
import app.tulz.tuplez.ApplyConverters
import calico.html.HtmlAttr
import calico.html.Modifier
import frontroute.internal.LocationState
import frontroute.internal.UrlString
import frontroute.ops.DirectiveOfOptionOps
import frontroute.internal.PathMatchResult
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
import cats.effect.OutcomeIO
import cats.effect.*
import cats.effect.std.Hotswap
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.*
import fs2.concurrent.*

import scala.annotation.targetName
import scala.util.chaining.*
import scala.scalajs.js
import scala.util.matching.Regex
import scala.util.matching.Regex.Match

type PathMatcher0 = PathMatcher[Unit]

type Directive0 = Directive[Unit]

def initRouting[N <: fs2.dom.Node[IO]]: InitRouting =
  InitRouting(LocationProvider.windowLocationProvider)

def initRouting[N <: fs2.dom.Node[IO]](lp: LocationProvider): InitRouting =
  InitRouting(lp)

def routes[M](mods: M)(using Modifier[IO, HtmlDivElement[IO], M]) =
  div(
    styleAttr := "display: contents",
    initRouting,
    mods
  )

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

def navigate(
  to: String,
  replace: Boolean = false,
): Route =
  extractMatchedPath { matched =>
    runEffect {
      if (replace) {
        BrowserNavigation.replaceState(url = makeRelative(matched, to))
      } else {
        BrowserNavigation.pushState(url = makeRelative(matched, to))
      }
    }
  }

case class NavMod[M](
  compare: (frontroute.Location, org.scalajs.dom.Location) => Boolean,
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
              fs2.Stream
                .eval(IO.cede >> LocationState.closestOrFail(e))
                .flatMap { locationState =>
                  mutations.stream
                    .evalMap(_ => locationState.location.get)
                    .merge(locationState.location.discrete)
                    .map { location =>
                      val UrlString(url) = e.asInstanceOf[dom.HTMLAnchorElement].href
                      location.exists { location =>
                        m.compare(location, url)
                      }
                    }
                }
                .holdResource(false)
                .flatMap { active =>
                  M.modify(m.mod(active), e)
                }
            }
        }
    }

def navModFn[M](
  compare: (frontroute.Location, org.scalajs.dom.Location) => Boolean
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

case class WithMatchedPathAndEl[N, M](
  body: (N, Signal[IO, List[String]]) => M
)

object WithMatchedPathAndEl:

  given [N <: fs2.dom.Node[IO], M](using M: Modifier[IO, N, M]): Modifier[IO, N, WithMatchedPathAndEl[N, M]] =
    (m, e) => {
      Resource
        .eval(
          SignallingRef.of[IO, List[String]](List.empty)
        ).flatMap { consumedVar =>
          Resource.eval(LocationState.closest(e)).flatMap {
            case None                =>
              Hotswap.create[IO, IO[OutcomeIO[Unit]]].flatMap { hs =>
                Resource.eval {
                  hs.swap(
                    fs2.Stream
                      .eval(IO.cede >> LocationState.closestOrFail(e))
                      .flatMap { closest =>
                        closest.consumed.discrete
                      }
                      .foreach { consumed =>
                        (IO.cede >> LocationState.closest(e)).flatMap {
                          case None                =>
                            consumedVar.set(consumed)
                          case Some(locationState) =>
                            hs.swap(
                              locationState.consumed.discrete
                                .foreach(consumedVar.set)
                                .compile.drain.background
                            ).void
                        }
                      }
                      .compile.drain.background
                  )
                }
              }
            case Some(locationState) =>
              locationState.consumed.discrete.foreach(consumedVar.set).compile.drain.background
          } >>
            M.modify(m.body(e, consumedVar), e)
        }
    }

private[frontroute] def withMatchedPathAndEl[N <: fs2.dom.Node[IO], M](
  body: (N, Signal[IO, List[String]]) => M
): WithMatchedPathAndEl[N, M] =
  WithMatchedPathAndEl(body)

def withMatchedPath[N <: fs2.dom.Node[IO], M](mod: Signal[IO, List[String]] => M): WithMatchedPathAndEl[N, M] =
  withMatchedPathAndEl((_, consumed) => mod(consumed))

def relativeHref(path: String): WithMatchedPathAndEl[HtmlAnchorElement[IO], HtmlAttr.SignalModifier[IO, String]] =
  withMatchedPath { matched =>
    href <-- matched.map { matched =>
      makeRelative(matched, path)
    }
  }

private[frontroute] object Frontroute:

  val history: History[IO, Unit] = Window[IO].history[Unit]
