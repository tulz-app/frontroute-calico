package io.frontroute.site.layout

import io.frontroute.site.Page
import io.frontroute.site.Site
import io.frontroute.site.SiteModule

import cats.syntax.all.*
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

object PageNavigation {

  def apply(
    page: Signal[IO, Option[(SiteModule, Page)]],
    mobile: Boolean = false,
    site: Site,
  )(
    hidden: Signal[IO, Boolean]
  ): Resource[IO, HtmlElement[IO]] = {
    val classes = List("py-4 overflow-auto bg-gray-800 text-white") ++ (if (mobile) List.empty else List("w-80 hidden lg:block"))
    navTag(
      cls <-- hidden.ifF("hidden" :: classes, classes),
      page.map {
        case None              => None
        case Some((module, _)) =>
          div(
            cls := List("space-y-4"),
            Option.when(module.index.title.nonEmpty) {
              navigationItem(page, module.index) {
                a(
                  cls  := List("ml-2 flex text-xl font-display font-bold"),
                  href := site.thisVersionHref(s"/${module.path}"),
                  (module.index.title: String)
                )
              }
            },
            module.navigation.map { case (title, pages) =>
              div(
                Option.when(title.nonEmpty) {
                  div(
                    cls := List("ml-4 text-xl font-display font-semibold text-gray-400 tracking-wide"),
                    title
                  )
                },
                pages.map { p =>
                  navigationItem(page, p)(
                    a(
                      cls  := List("ml-6 flex font-display font-medium tracking-wide"),
                      href := site.thisVersionHref(s"/${module.path}/${p.link}"),
                      (p.title: String)
                    )
                  )
                }
              )
            }
          ).some
      }
    )
  }

  private def navigationItem[M](
    currentPage: Signal[IO, Option[(SiteModule, Page)]],
    page: Page
  )(mods: M)(using Modifier[IO, HtmlDivElement[IO], M]): Resource[IO, HtmlElement[IO]] =
    div(
//      cls := List("px-2 py-1"),
      cls <-- currentPage
        .map(_.exists(_._2.path == page.path)).ifF(
          List("px-2 py-1 text-white bg-gray-700"),
          List("px-2 py-1 text-gray-200 hover:text-white hover:bg-gray-700")
        ),
      mods
    )

}
