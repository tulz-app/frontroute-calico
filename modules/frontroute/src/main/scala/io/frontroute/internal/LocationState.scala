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
import cats.effect.Resource
import cats.effect.IO
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef

import scala.annotation.tailrec
import scala.scalajs.js
import scala.scalajs.js.UndefOr

@js.native
private[internal] trait ElementWithLocationState extends js.Any {

  var ____locationState: js.UndefOr[Resource[IO, LocationState]]

}

private[frontroute] class RouterStateRef {

  private var states: Map[Route, RoutingState] = Map.empty

  def get(r: Route): Option[RoutingState] = states.get(r)

  def set(r: Route, next: RoutingState): Unit = {
    states = states.updated(r, next)
  }

  def unset(r: Route): Unit = {
    states = states.removed(r)
  }

}

private[frontroute] object LocationState:

  def default: Resource[IO, LocationState] = withLocationProvider(LocationProvider.windowLocationProvider)

  def apply(
    _location: Signal[IO, Option[Location]],
    _isSiblingMatched: () => Boolean,
    _resetSiblingMatched: () => Unit,
    _notifySiblingMatched: () => Unit,
    _routerState: RouterStateRef
  ): IO[LocationState] =
    (
      SignallingRef[IO, Option[Location]](Option.empty[Location]),
      SignallingRef[IO, List[String]](List.empty[String]),
    ).mapN { (remainingVar, consumedVar) =>
      var _childMatched = false

      new LocationState {

        val location: Signal[IO, Option[Location]] = _location
        def isSiblingMatched: Boolean        = _isSiblingMatched()

        def resetSiblingMatched(): Unit = {
          _resetSiblingMatched()
        }

        def notifySiblingMatched(): Unit            = _notifySiblingMatched()
        val routerState: RouterStateRef             = _routerState
        val remaining: Signal[IO, Option[Location]] = remainingVar

        def setRemaining(remaining: Option[Location]): IO[Unit] = {
          remainingVar.set(remaining)
        }

        val consumed: Signal[IO, List[String]] = consumedVar

        def setConsumed(consumed: List[String]): IO[Unit] = {
          consumedVar.set(consumed)
        }

        def notifyChildMatched(): Unit = {
          _childMatched = true
        }

        def resetChildMatched(): Unit = {
          _childMatched = false
        }

        def isChildMatched: Boolean = _childMatched

      }

    }

  def withLocationProvider(lp: LocationProvider): Resource[IO, LocationState] = {
    var siblingMatched = false
    lp.current.discrete
      .evalTap { _ =>
        IO.delay {
          siblingMatched = false
        }
      }
      .compile
      .drain
      .background >>
      Resource.eval {
        LocationState(
          _location = lp.current,
          _isSiblingMatched = () => {
            siblingMatched
          },
          _resetSiblingMatched = () => {
            siblingMatched = false
          },
          _notifySiblingMatched = () => {
            siblingMatched = true
          },
          _routerState = new RouterStateRef,
        )
      }

  }

  def forNode[N <: fs2.dom.Node[IO]](n: N): js.UndefOr[Resource[IO, LocationState]] = {
    n.asInstanceOf[ElementWithLocationState].____locationState
  }

  @tailrec
  def closestOrDefault[N <: fs2.dom.Node[IO]](n: N): Resource[IO, LocationState] = {
    val node      = n.asInstanceOf[dom.Node]
    val withState = node.asInstanceOf[ElementWithLocationState]
    if (withState.____locationState.isEmpty) {
      if (node.parentNode != null) {
        closestOrDefault(node.parentNode.asInstanceOf[N])
      } else {
        withState.____locationState = default
        default
      }
    } else {
      withState.____locationState.get
    }
  }

  @tailrec
  def closest[N <: fs2.dom.Node[IO]](n: N): Option[Resource[IO, LocationState]] = {
    val node      = n.asInstanceOf[dom.Node]
    val withState = node.asInstanceOf[ElementWithLocationState]
    if (withState.____locationState.isEmpty) {
      if (node.parentNode != null) {
        closest(node.parentNode.asInstanceOf[N])
      } else {
        Option.empty
      }
    } else {
      Some(withState.____locationState.get)
    }
  }

  def initIfMissing[N <: fs2.dom.Node[IO]](n: N, init: () => Resource[IO, LocationState]): Resource[IO, LocationState] = {
    val node            = n.asInstanceOf[dom.Node]
    val resultWithState = node.asInstanceOf[ElementWithLocationState]
    if (resultWithState.____locationState.isEmpty) {
      resultWithState.____locationState = init()
    }
    resultWithState.____locationState.get
  }

private[frontroute] trait LocationState {

  val location: Signal[IO, Option[Location]]
  def isSiblingMatched: Boolean
  def resetSiblingMatched(): Unit
  def notifySiblingMatched(): Unit
  val routerState: RouterStateRef
  val remaining: Signal[IO, Option[Location]]
  def setRemaining(remaining: Option[Location]): IO[Unit]
  val consumed: Signal[IO, List[String]]
  def setConsumed(consumed: List[String]): IO[Unit]
  def notifyChildMatched(): Unit
  def resetChildMatched(): Unit
  def isChildMatched: Boolean

}
