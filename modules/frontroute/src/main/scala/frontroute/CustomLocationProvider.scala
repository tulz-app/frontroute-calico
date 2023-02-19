package frontroute

import cats.Functor
import cats.effect.IO
import fs2.concurrent.Signal
import frontroute.internal.UrlString

import scala.scalajs.js

class CustomLocationProvider(locationStrings: Signal[IO, String]) extends LocationProvider {

  val current: Signal[IO, Option[Location]] = locationStrings.map { case UrlString(location) =>
    Some(Location(location, Option.empty))
  }

}
