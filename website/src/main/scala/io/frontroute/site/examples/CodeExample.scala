package io.frontroute.site.examples

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

abstract class CodeExample(
  val id: String,
  val title: String,
  val description: String,
  val links: Seq[String]
)(
  _code: sourcecode.Text[() => Resource[IO, HtmlElement[IO]]]
) {

  val code: sourcecode.Text[() => Resource[IO, HtmlElement[IO]]] = _code

}
