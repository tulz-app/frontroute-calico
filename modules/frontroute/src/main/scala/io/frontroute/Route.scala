package io.frontroute

import cats.syntax.all.*
import cats.effect.syntax.all.*
import calico.html.Modifier
import io.frontroute.internal.LocationState
import io.frontroute.internal.RoutingState
import io.frontroute.internal.RouterStateRef
import calico.*
import calico.html.io.given
import calico.html.*
import calico.syntax.*
import calico.html.Children.ResourceListSignalModifier
import cats.effect.Async
import cats.effect.IO
import cats.effect.Resource
import cats.effect.kernel.Resource
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlElement
import scala.concurrent.duration._

trait Route extends ((Location, RoutingState, RoutingState) => IO[RouteResult])

object Route {

  given modifierForRoute[N <: fs2.dom.Node[IO]]: Modifier[IO, N, Route] = { (m, e) =>
    // println(s"modifierForRoute: $m")
    Resource
      .eval(
        (
          SignallingRef.of[IO, Option[Resource[IO, fs2.dom.Node[IO]]]](Option.empty),
          SignallingRef.of[IO, Option[LocationState]](Option.empty),
          RouterStateRef()
        ).tupled
      )
      .flatMap { case (currentRender, currentRenderLocationState, childStateRef) =>
        (
          IO.cede >>
            LocationState
              .closestOrDefault(e)
              .flatMap { locationState =>

                locationState.location.discrete
                  .evalMap {
                    case Some(currentUnmatched) =>
                      (
                        locationState.consumed.get,
                        currentRender.get,
                        locationState.isSiblingMatched,
                        locationState.routerState.get(m)
                      ).tupled.flatMap { (consumedNow, currentRenderNow, isSiblingMatched, routerState) =>
                        m.apply(
                          currentUnmatched.copy(otherMatched = isSiblingMatched),
                          routerState.fold(RoutingState.empty)(_.resetPath),
                          RoutingState.empty.withConsumed(consumedNow)
                        ).flatMap {
                            case RouteResult.Matched(nextState, location, consumed, createResult) =>
                              locationState.resetChildMatched >>
                                locationState.notifySiblingMatched >> (
                                  if (
                                    !routerState.contains(nextState) ||
                                    currentRenderNow.isEmpty
                                  ) {
                                    RouteEvent.NextRender(nextState, location, consumed, createResult).pure[IO]
                                  } else {
                                    RouteEvent.SameRender(nextState, location, consumed).pure[IO]
                                  }
                                )
                            case RouteResult.RunEffect(nextState, location, consumed, run)        =>
                              locationState.notifySiblingMatched >> (
                                if (!routerState.contains(nextState)) {
                                  run >>
                                    RouteEvent.SameRender(nextState, location, consumed).pure[IO]
                                } else {
                                  RouteEvent.SameRender(nextState, location, consumed).pure[IO]
                                }
                              )

                            case RouteResult.Rejected =>
                              RouteEvent.NoRender.pure[IO]
                          }.flatMap {
                            case RouteEvent.NextRender(nextState, remaining, consumed, render) =>
                              locationState.routerState.set(m, nextState) >>
                                locationState.setRemaining(Some(remaining)) >>
                                currentRender.set(
                                  Some(
                                    render.flatTap { element =>
                                      Resource.eval {
                                        LocationState
                                          .initIfMissing(
                                            element,
                                            Resource.eval {
                                              LocationState(
                                                _location = locationState.remaining,
                                                _isSiblingMatched = locationState.isChildMatched,
                                                _resetSiblingMatched = locationState.resetChildMatched,
                                                _notifySiblingMatched = locationState.notifyChildMatched,
                                                _routerState = childStateRef,
                                              )
                                            }
                                          )
                                          .flatMap { childState =>
                                            childState.setConsumed(consumed) >>
                                              currentRenderLocationState.set(childState.some)
                                          }
                                      }
                                    }
                                  )
                                )

                            case RouteEvent.SameRender(nextState, remaining, consumed) =>
                              locationState.routerState.set(m, nextState) >>
                                currentRenderLocationState.get.flatMap {
                                  case Some(state) =>
                                    state.setConsumed(consumed)
                                  case None        =>
                                    IO.unit
                                  // render.ref.dataset.addOne("frPath" -> consumed.mkString("/", "/", ""))
                                } >>
                                locationState.setRemaining(Some(remaining))
                            case RouteEvent.NoRender                                   =>
                              locationState.routerState.unset(m) >>
                                currentRender.set(None) >>
                                currentRenderLocationState.set(None)
                          }
                      }
                    case None                   =>
                      IO.unit
                    //              locationState.routerState.unset(this)
                    //              currentRender.set(None)
                  }.compile.drain
              }
        ).background >> forNodeOptionSignal.modify(currentRender, e)
      }
  }

  implicit def toDirective[L](route: Route): Directive[L] = Directive[L](_ => route)

  sealed private[frontroute] trait RouteEvent extends Product with Serializable

  private[frontroute] object RouteEvent {

    case class NextRender[N <: fs2.dom.Node[IO]](
      nextState: RoutingState,
      remaining: Location,
      nextConsumed: List[String],
      render: Resource[IO, N]
    ) extends RouteEvent

    case class SameRender(
      nextState: RoutingState,
      remaining: Location,
      nextConsumed: List[String],
    ) extends RouteEvent

    case object NoRender extends RouteEvent

  }

}
