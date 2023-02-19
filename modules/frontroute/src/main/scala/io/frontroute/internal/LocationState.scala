package io.frontroute
package internal

import cats.syntax.all.*
import cats.effect.syntax.all.*
import org.scalajs.dom
import calico.*
import calico.html.io.given
import calico.html.io.*
import calico.syntax.*
import cats.effect.Async
import cats.effect.IO
import cats.effect.Ref
import cats.effect.Resource
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef

import scala.annotation.tailrec
import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.JSConverters._

@js.native
private[internal] trait ElementWithLocationState extends js.Any {

  var ____locationState: js.UndefOr[LocationState]

}

private[frontroute] class RouterStateRef(
  states: Ref[IO, Map[Route, RoutingState]]
) {

  def get(r: Route): IO[Option[RoutingState]] = states.get.map(_.get(r))

  def set(r: Route, next: RoutingState): IO[Unit] = {
    states.update(_.updated(r, next))
  }

  def unset(r: Route): IO[Unit] = {
    states.update(_.removed(r))
  }

}

private[frontroute] object RouterStateRef:

  def apply(): IO[RouterStateRef] =
    IO.ref(Map.empty[Route, RoutingState]).map(new RouterStateRef(_))

private[frontroute] object LocationState:

  def apply(
    _location: Signal[IO, Option[Location]],
    _isSiblingMatched: IO[Boolean],
    _resetSiblingMatched: IO[Unit],
    _notifySiblingMatched: IO[Unit],
    _routerState: RouterStateRef
  ): IO[LocationState] =
    (
      SignallingRef[IO, Option[Location]](Option.empty[Location]),
      SignallingRef[IO, List[String]](List.empty[String]),
      SignallingRef[IO, Boolean](false),
    ).mapN { (remainingVar, consumedVar, childMatchedVar) =>
      new LocationState {

        val location: Signal[IO, Option[Location]] = _location
        val isSiblingMatched: IO[Boolean]          = _isSiblingMatched

        val resetSiblingMatched: IO[Unit] = _resetSiblingMatched

        val notifySiblingMatched: IO[Unit]          = _notifySiblingMatched
        val routerState: RouterStateRef             = _routerState
        val remaining: Signal[IO, Option[Location]] = remainingVar

        def setRemaining(remaining: Option[Location]): IO[Unit] = remainingVar.set(remaining)

        val consumed: Signal[IO, List[String]] = consumedVar

        def setConsumed(consumed: List[String]): IO[Unit] = consumedVar.set(consumed)

        val notifyChildMatched: IO[Unit] = childMatchedVar.set(true)

        val resetChildMatched: IO[Unit] = childMatchedVar.set(false)

        val isChildMatched: IO[Boolean] = childMatchedVar.get

      }

    }

  private[frontroute] def withLocationProvider(lp: LocationProvider): Resource[IO, LocationState] =
    Resource.eval(SignallingRef.of[IO, Boolean](false)).flatMap { siblingMatchedVar =>
      lp.current.discrete
        .evalTap { _ =>
          siblingMatchedVar.set(false)
        }
        .compile
        .drain
        .background >>
        Resource.eval {
          RouterStateRef().flatMap { state =>
            LocationState(
              _location = lp.current,
              _isSiblingMatched = siblingMatchedVar.get,
              _resetSiblingMatched = siblingMatchedVar.set(false),
              _notifySiblingMatched = siblingMatchedVar.set(true),
              _routerState = state,
            )
          }
        }
    }

  def closestOrFail[N <: fs2.dom.Node[IO]](n: N): IO[LocationState] = {
    val node      = n.asInstanceOf[dom.Node]
    val withState = node.asInstanceOf[ElementWithLocationState]
    IO(withState.____locationState).flatMap { locationState =>
      if (locationState.isEmpty) {
        if (node.parentNode != null) {
          dom.console.log("no location state, have parent", n)
          closestOrFail(node.parentNode.asInstanceOf[N])
        } else {
          dom.console.log("no location state, no parent", n)
          dom.console.log("no location state", n)
          IO.raiseError(new RuntimeException("location provider not configured"))
        }
      } else {
//      dom.console.log("closest found!", n)
        withState.____locationState.get.pure[IO]
      }
    }
  }

  @tailrec
  def closest[N <: fs2.dom.Node[IO]](n: N): IO[Option[LocationState]] = {
    val node      = n.asInstanceOf[dom.Node]
    val withState = node.asInstanceOf[ElementWithLocationState]
    if (withState.____locationState.isEmpty) {
      if (node.parentNode != null) {
        closest(node.parentNode.asInstanceOf[N])
      } else {
        Option.empty.pure[IO]
      }
    } else {
      Some(withState.____locationState.get).pure[IO]
    }
  }

  def init[N <: fs2.dom.Node[IO]](n: N, init: LocationState): IO[Unit] = {
    val node            = n.asInstanceOf[dom.Node]
    val resultWithState = node.asInstanceOf[ElementWithLocationState]
    if (resultWithState.____locationState.isEmpty) {
      dom.console.log("initializing location state", n)
      IO {
//          dom.console.log("init location state", node)
//          node.asInstanceOf[dom.HTMLElement].dataset.addOne("frLocationState", "initialized")
        resultWithState.____locationState = init
      }
    } else {
      IO.raiseError(new RuntimeException("location state already initialized"))
    }
  }

private[frontroute] trait LocationState {

  val location: Signal[IO, Option[Location]]
  val isSiblingMatched: IO[Boolean]
  val resetSiblingMatched: IO[Unit]
  val notifySiblingMatched: IO[Unit]
  val routerState: RouterStateRef
  val remaining: Signal[IO, Option[Location]]
  def setRemaining(remaining: Option[Location]): IO[Unit]
  val consumed: Signal[IO, List[String]]
  def setConsumed(consumed: List[String]): IO[Unit]
  val notifyChildMatched: IO[Unit]
  val resetChildMatched: IO[Unit]
  val isChildMatched: IO[Boolean]

}
