package io.frontroute

import io.frontroute.internal.RoutingState
import cats.effect.IO
import cats.effect.Resource
import fs2.dom.HtmlElement

enum RouteResult:
  case Matched(state: RoutingState, location: Location, consumed: List[String], result: Resource[IO, HtmlElement[IO]])
  case RunEffect(state: RoutingState, location: Location, consumed: List[String], run: IO[Unit])
  case Rejected
