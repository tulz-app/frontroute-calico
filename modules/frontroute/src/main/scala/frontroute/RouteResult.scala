package frontroute

import frontroute.internal.RoutingState
import cats.effect.IO
import cats.effect.Resource
import fs2.dom.HtmlElement

enum RouteResult:
  case Matched[N <: fs2.dom.Node[IO]](state: RoutingState, location: Location, consumed: List[String], result: Resource[IO, N])
  case RunEffect(state: RoutingState, location: Location, consumed: List[String], run: IO[Unit])
  case Rejected
