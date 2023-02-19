package io.frontroute

import cats.syntax.all.*
import calico.IOWebApp
import calico.syntax.*
import calico.unsafe.given
import cats.effect.*
import fs2.dom.Window
import io.frontroute.internal.LocationState
import io.frontroute.internal.LocationState.withLocationProvider

private[frontroute] object IORoutesApp:

  private var _defaultLocationState: LocationState = _

  def defaultLocationState: LocationState = _defaultLocationState

trait IORoutesApp extends IOWebApp:

  def defaultLocationProvider: LocationProvider = LocationProvider.windowLocationProvider

  def renderRoutes: Resource[IO, fs2.dom.HtmlElement[IO]]

  final def render: Resource[IO, fs2.dom.HtmlElement[IO]] =
    withLocationProvider(defaultLocationProvider).flatMap { defaultLocationState =>
      Resource.eval {
        IO {
          IORoutesApp._defaultLocationState = defaultLocationState
        }
      } >> renderRoutes
    }
