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

final class Page(
  val path: String,
  val link: String,
  val title: String,
  val render: Resource[IO, HtmlElement[IO]]
)
