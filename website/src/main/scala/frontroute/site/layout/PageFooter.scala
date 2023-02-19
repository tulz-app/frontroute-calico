package frontroute.site.layout

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

object PageFooter {

  def apply(): Resource[IO, HtmlElement[IO]] =
    div(
      cls := "hidden bg-gray-900 text-white p-4"
    )

}
