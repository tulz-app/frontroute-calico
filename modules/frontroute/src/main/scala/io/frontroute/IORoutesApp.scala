package io.frontroute

import cats.syntax.all.*
import calico.IOWebApp
import calico.syntax.*
import calico.unsafe.given
import cats.effect.*
import fs2.dom.Window
import io.frontroute.internal.LocationState
import io.frontroute.internal.LocationState.withLocationProvider

trait IORoutesApp extends IOWebApp:

  def renderRoutes: Resource[IO, fs2.dom.HtmlElement[IO]]

  final def render: Resource[IO, fs2.dom.HtmlElement[IO]] =
    renderRoutes
