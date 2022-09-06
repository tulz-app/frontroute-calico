package io.frontroute

import com.raquo.laminar.api.L._
import io.frontroute.internal.RoutingState

sealed trait RouteResult extends Product with Serializable

object RouteResult {
  final case class Matched(state: RoutingState, location: Location, result: () => Signal[Element]) extends RouteResult
  case object Rejected                                                                             extends RouteResult
}
