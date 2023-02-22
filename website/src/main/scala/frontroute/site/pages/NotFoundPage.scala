package frontroute.site.pages

import frontroute.site.components.DocumentationDisplay
import frontroute.site.Site
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

object NotFoundPage {

  def apply(
    title: String,
    markdown: String,
  ): Resource[IO, HtmlElement[IO]] = page(title) {
    DocumentationDisplay(title, markdown)
  }

}
