package io.frontroute

import app.tulz.tuplez.ApplyConverter
import app.tulz.tuplez.ApplyConverters
import calico.html.HtmlAttr
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

private[frontroute] val rejected: IO[RouteResult] = IO.pure(RouteResult.Rejected)

private[frontroute] def extractContext: Directive[io.frontroute.Location] =
  Directive[io.frontroute.Location](inner => (location, previous, state) => inner(location)(location, previous, state))

private[frontroute] def extract[T](f: io.frontroute.Location => T): Directive[T] =
  extractContext.map(f)

implicit def directiveOfOptionSyntax[L](underlying: Directive[Option[L]]): DirectiveOfOptionOps[L] = new DirectiveOfOptionOps(underlying)

extension (directive: Directive0)
  def apply(subRoute: Route): Route =
    (location, previous, state) => directive.tapply(_ => subRoute)(location, previous, state)

extension [L](directive: Directive[L])
  def apply(subRoute: L => Route): Route =
    (location, previous, state) => directive.tapply(subRoute)(location, previous, state)

extension (directive: Directive0)
  def execute(effect: IO[Unit]): Route =
    (location, previous, state) => directive.tapply(_ => runEffect { effect })(location, previous, state)

extension [L](directive: Directive[L])
  def execute(effect: L => IO[Unit]): Route =
    (location, previous, state) =>
      directive.tapply(l =>
        runEffect {
          effect(l)
        }
      )(location, previous, state)

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
