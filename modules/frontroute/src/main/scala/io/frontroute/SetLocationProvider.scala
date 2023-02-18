package io.frontroute

import io.frontroute.internal.LocationState
import cats.syntax.all.*
import calico.*
import calico.html.io.given
import calico.html.*
import calico.html.io.*
import calico.syntax.*
import cats.effect.IO
import cats.effect.Resource

class SetLocationProvider(private val lp: LocationProvider)

object SetLocationProvider:

  given modifierForLocationProvider[N <: fs2.dom.Node[IO]]: Modifier[IO, N, SetLocationProvider] =
    (m, e) =>
      LocationState.forNode(e).toOption match {
        case Some(_) =>
          Resource.raiseError(
            throw new IllegalStateException("initializing location provider: location state is already defined")
          )
        case None    =>
          LocationState
            .initIfMissing(
              e,
              LocationState.withLocationProvider(m.lp)
            ).void
      }
