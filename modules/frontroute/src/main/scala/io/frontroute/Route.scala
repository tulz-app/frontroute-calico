package io.frontroute

import calico.html.Modifier
import io.frontroute.internal.LocationState
import io.frontroute.internal.RoutingState
import io.frontroute.internal.RouterStateRef
import calico.*
import calico.html.*
import calico.syntax.*
import calico.html.Children.ResourceListSignalModifier
import cats.effect.Async
import cats.effect.IO
import cats.effect.kernel.Resource
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlElement

trait Route extends ((Location, RoutingState, RoutingState) => RouteResult)

object Route {

  given modifierForRoute[N <: fs2.dom.Node[IO]]: Modifier[IO, N, Route] = { (m, e) =>
    ???
//    private val currentRender = SignallingRef(Option.empty[fs2.dom.Node[F]])
    //    private def bind: Resource[F, Unit] = {
    //      Binder { el =>
    //        ReactiveElement.bindCallback(el) { ctx =>
    //          val locationState = LocationState.closestOrDefault(el.ref)
    //          val childStateRef = new RouterStateRef
    //
    //          // the returned subscription will be managed by the ctx.owner
    //          val _ = locationState.location
    //            .foreach {
    //              case Some(currentUnmatched) =>
    //                val renderResult = this.apply(
    //                  currentUnmatched.copy(otherMatched = locationState.isSiblingMatched()),
    //                  locationState.routerState.get(this).fold(RoutingState.empty)(_.resetPath),
    //                  RoutingState.empty.withConsumed(locationState.consumed.now())
    //                ) match {
    //                  case RouteResult.Matched(nextState, location, consumed, createResult) =>
    //                    locationState.resetChildMatched()
    //                    locationState.notifySiblingMatched()
    //                    if (
    //                      !locationState.routerState.get(this).contains(nextState) ||
    //                        currentRender.now().isEmpty
    //                    ) {
    //                      RouteEvent.NextRender(nextState, location, consumed, createResult())
    //                    } else {
    //                      RouteEvent.SameRender(nextState, location, consumed)
    //                    }
    //                  case RouteResult.RunEffect(nextState, location, consumed, run) =>
    //                    locationState.notifySiblingMatched()
    //                    if (!locationState.routerState.get(this).contains(nextState)) {
    //                      run()
    //                      RouteEvent.SameRender(nextState, location, consumed)
    //                    } else {
    //                      RouteEvent.SameRender(nextState, location, consumed)
    //                    }
    //
    //                  case RouteResult.Rejected =>
    //                    RouteEvent.NoRender
    //                }
    //                renderResult match {
    //                  case RouteEvent.NextRender(nextState, remaining, consumed, render) =>
    //                    locationState.routerState.set(this, nextState)
    //
    //                    locationState.setRemaining(Some(remaining))
    //                    if (render != null) {
    //                      val childState = LocationState.initIfMissing(
    //                        render.ref,
    //                        () =>
    //                          new LocationState(
    //                            location = locationState.remaining,
    //                            isSiblingMatched = locationState.isChildMatched,
    //                            resetSiblingMatched = locationState.resetChildMatched,
    //                            notifySiblingMatched = locationState.notifyChildMatched,
    //                            routerState = childStateRef,
    //                          )
    //                      )
    //                      childState.setConsumed(consumed)
    //
    //                      render.ref.dataset.addOne("frPath" -> consumed.mkString("/", "/", ""))
    //                      currentRender.set(Some(render))
    //                    } else {
    //                      currentRender.set(None) // route matched but rendered a null
    //                    }
    //                  case RouteEvent.SameRender(nextState, remaining, consumed) =>
    //                    locationState.routerState.set(this, nextState)
    //                    currentRender.now().foreach { render =>
    //                      LocationState.closestOrDefault(render.ref).setConsumed(consumed)
    //                      render.ref.dataset.addOne("frPath" -> consumed.mkString("/", "/", ""))
    //                    }
    //
    //                    locationState.setRemaining(Some(remaining))
    //                  case RouteEvent.NoRender =>
    //                    locationState.routerState.unset(this)
    //                    currentRender.set(None)
    //                }
    //              case None =>
    //              //              locationState.routerState.unset(this)
    //              //              currentRender.set(None)
    //            }(ctx.owner)
    //        }
    //      }
    //    }
    //
    //
    //    e.amend(
    //      child.maybe <-- currentRender.signal,
    //      bind,
    //    )
  }

  implicit def toDirective[L](route: Route): Directive[L] = Directive[L](_ => route)

  sealed private[frontroute] trait RouteEvent extends Product with Serializable

  private[frontroute] object RouteEvent {

    case class NextRender(
      nextState: RoutingState,
      remaining: Location,
      nextConsumed: List[String],
      render: HtmlElement[IO]
    ) extends RouteEvent

    case class SameRender(
      nextState: RoutingState,
      remaining: Location,
      nextConsumed: List[String],
    ) extends RouteEvent

    case object NoRender extends RouteEvent

  }

}
