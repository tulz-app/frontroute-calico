package frontroute

import app.tulz.tuplez.ApplyConverter
import app.tulz.tuplez.ApplyConverters
import calico.*
import calico.html.HtmlAttr
import calico.html.Modifier
import calico.html.io.given
import calico.html.io.*
import calico.syntax.*
import cats.effect.*
import cats.effect.std.Hotswap
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.*
import fs2.concurrent.*
import fs2.dom.*
import frontroute.internal.LocationState
import frontroute.internal.PathMatchResult
import frontroute.internal.UrlString
import frontroute.ops.DirectiveOfOptionOps
import org.scalajs.dom

import scala.annotation.targetName
import scala.scalajs.js
import scala.util.NotGiven
import scala.util.chaining.*
import scala.util.matching.Regex
import scala.util.matching.Regex.Match

case class InitRouting(lp: LocationProvider)

object InitRouting:

  given [N <: fs2.dom.Node[IO]]: Modifier[IO, N, InitRouting] =
    (m, e) =>
      LocationState
        .withLocationProvider(m.lp).evalMap { locationState =>
          LocationState.init(e, locationState).void
        }
