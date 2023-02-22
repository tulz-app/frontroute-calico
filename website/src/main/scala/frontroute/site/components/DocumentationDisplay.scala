package frontroute.site.components

import io.laminext.highlight.Highlight
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
import org.scalajs.dom

object DocumentationDisplay {

  def apply(
    title: String,
    docHtml: String,
  ): Resource[IO, HtmlElement[IO]] =
    div(
      cls := "space-y-4",
      h1(
        cls := "font-display text-3xl font-bold text-gray-900 tracking-wider md:hidden",
        title
      ),
      div(
        cls := "prose prose-blue max-w-none",
      ).flatTap { e =>
        val node = e.asInstanceOf[dom.HTMLDivElement]
        Resource.eval {
          IO {
            node.innerHTML = docHtml
          } >> IO {
            node.querySelectorAll("pre > code").foreach { codeElement =>
              Highlight.highlightElement(codeElement)
            }
          }
        }
      }
    )

}
