package frontroute.site

import frontroute.*
import frontroute.site.components.CodeExampleDisplay
import io.laminext.highlight.Highlight
import io.laminext.highlight.HighlightJavaScript
import io.laminext.highlight.HighlightJson
import io.laminext.highlight.HighlightScala
import io.laminext.highlight.HighlightXml
import org.scalajs.dom
import calico.*
import calico.html.io.given
import calico.html.io.*
import fs2.dom.*
import calico.syntax.*
import cats.effect.*
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.*
import fs2.concurrent.*

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("stylesheets/index.css", JSImport.Namespace)
object IndexCss extends js.Object

object Main extends IOWebApp {

  val indexCss: IndexCss.type = IndexCss

  private def renderExample(
    highlightStyle: SignallingRef[IO, String],
    site: Site,
  ): Resource[IO, HtmlDivElement[IO]] = {
    val id = dom.window.location.pathname.drop(site.thisVersionHref("/example-frame/").length).takeWhile(_ != '/')
    site.examples
      .find(_.id == id)
      .map(ex => CodeExampleDisplay.frame(ex))
      .getOrElse(div(s"EXAMPLE NOT FOUND: ${id}"))
  }

  private def insertJsClass(ssrContext: SsrContext): Unit = {
    if (!ssrContext.ssr) {
      val style = dom.document.createElement("style").asInstanceOf[dom.html.Style]
      style.`type` = "text/css"
      style.innerHTML = s""".hidden-if-js{display: none;}""".stripMargin
      val _     = dom.document.getElementsByTagName("head")(0).appendChild(style)
    }
  }

  private def removeNoJsClass(ssrContext: SsrContext): Unit = {
    if (!ssrContext.ssr) {
      Option(dom.document.head.querySelector("style#no-js")).foreach(dom.document.head.removeChild(_))
    }
  }

  def render = {
//    Theme.setTheme(DefaultTheme.theme)
//    Modal.initialize()
    Highlight.registerLanguage("scala", HighlightScala)
    Highlight.registerLanguage("javascript", HighlightJavaScript)
    Highlight.registerLanguage("json", HighlightJson)
    Highlight.registerLanguage("html", HighlightXml)
    Resource.eval(SignallingRef[IO, String]("an-old-hope")).flatMap { highlightStyle =>
      val site   = Site(highlightStyle)
      val wiring = Wiring(site)
      Resource.eval {
        IO {
          removeNoJsClass(wiring.ssrContext)
          insertJsClass(wiring.ssrContext)

        }
      } >> (
        if (dom.window.location.pathname.startsWith(site.thisVersionHref("/example-frame/"))) {
          renderExample(highlightStyle, site)
        } else {
          wiring.routes(highlightStyle)
        }
      )
    }
  }

  override def rootElementId = "app-container"

}
