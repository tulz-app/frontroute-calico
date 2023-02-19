package io.frontroute.site

import io.frontroute.*
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

package object pages {

  def page(
    title: String,
    description: Option[String] = None,
    keywords: Option[String] = None,
    status: PageStatusCode = PageStatusCode.Ok
  )(
    content: Resource[IO, HtmlElement[IO]]
  ): Resource[IO, HtmlElement[IO]] =
    content.flatTap { el =>
      Resource.eval {
        DocumentMeta.set(
          title = title,
          description = description,
          keywords = keywords,
          status = status
        )
      }
    }

}
