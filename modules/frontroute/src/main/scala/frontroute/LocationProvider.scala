package frontroute

import frontroute.internal.HistoryState
import cats.syntax.all.*
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import org.scalajs.dom
import calico.*
import calico.html.*
import calico.syntax.*
import cats.Functor
import cats.effect.Resource
import cats.effect.IO
import fs2.dom.Serializer

import scala.scalajs.js

trait LocationProvider {

  def current: Signal[IO, Option[Location]]

}

object LocationProvider:

  lazy val windowLocationProvider: LocationProvider =
    BrowserLocationProvider(Frontroute.history.state.map(_.map(_.asInstanceOf[js.UndefOr[js.Any]])))

//  @inline def custom(locationStrings: Signal[F, String])(using Functor[F]) = CustomLocationProvider[F](locationStrings)

object BrowserLocationProvider:

  def apply(state: Signal[IO, Option[js.UndefOr[js.Any]]]): LocationProvider =
    new LocationProvider:
      val current: Signal[IO, Option[Location]] = state.map { state =>
        Location(dom.window.location, state).some
      }
