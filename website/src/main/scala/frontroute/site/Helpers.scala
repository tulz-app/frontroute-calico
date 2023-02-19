package frontroute.site

import calico.*
import calico.html.*
import calico.html.io.given
import calico.html.io.*
import fs2.dom.*
import calico.syntax.*
import cats.effect.*
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.*
import fs2.concurrent.*
import shapeless3.deriving.K0

def wrap[M <: Tuple](modifiers: M)(using Modifier[IO, HtmlDivElement[IO], M]): Resource[IO, HtmlDivElement[IO]] = div(
  cls := "contents",
  modifiers
)
