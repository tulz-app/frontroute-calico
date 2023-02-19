package io.frontroute.site.pages

import io.frontroute.site.components.CodeExampleDisplay
import io.frontroute.site.examples.CodeExample
import io.frontroute.site.Site
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

object CodeExamplePage {

  def apply(
    example: CodeExample,
    site: Site,
    highlightStyle: Signal[IO, String]
  ): Resource[IO, HtmlElement[IO]] = page(example.title) {
    CodeExampleDisplay(example, site, highlightStyle)
  }

}
