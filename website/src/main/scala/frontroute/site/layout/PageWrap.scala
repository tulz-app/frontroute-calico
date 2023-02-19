package frontroute.site.layout

import frontroute.site.Page
import frontroute.site.Site
import frontroute.site.SiteModule
import frontroute.site.Styles
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
import org.scalajs.dom.HTMLLinkElement

object PageWrap {

  def apply(
    page: Signal[IO, Option[(SiteModule, Page)]],
    menuObserver: SignallingRef[IO, Option[Resource[IO, HtmlElement[IO]]]],
    site: Site,
    highlightStyle: SignallingRef[IO, String]
  ): Resource[IO, HtmlElement[IO]] = {
    div(
      linkTag(
//        rel := "stylesheet",
        href <-- highlightStyle.map(s => site.thisVersionHref(s"/stylesheets/highlightjs/${s}.css"))
      ).flatTap { e =>
        Resource.eval {
          IO {
            e.asInstanceOf[HTMLLinkElement].rel = "stylesheet"
          }
        }
      },
      div(
        cls := "h-screen flex flex-col",
        PageHeader(page, menuObserver, site, highlightStyle),
        noScriptTag(
          div(
            cls := "max-w-5xl border-l-4 border-red-400 bg-red-50 text-red-900 mx-auto p-4 font-condensed",
            "Your browser does not support JavaScript: some features of this site may not work properly."
          )
        ),
        div(
          cls := "flex-1 flex overflow-hidden",
          PageNavigation(page, site = site)(
            hidden = page.map(_.exists(_._1 == site.indexModule)),
          ),
          div(
            cls := "flex-1 bg-gray-200 overflow-auto md:p-4",
            div(
              cls := "lg:container lg:mx-auto lg:max-w-4xl lg:p-8 p-4 bg-white min-h-full flex flex-col",
              page.map {
                case Some((_, page)) => page.render.some
                case None            => none
              }
            )
          )
        ),
        PageFooter()
      )
    )
  }

}
