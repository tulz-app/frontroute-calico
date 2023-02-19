package io.frontroute.site

package layout

import io.frontroute.site.icons.Icons
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

object PageHeader {

  def apply(
    page: Signal[IO, Option[(SiteModule, Page)]],
    menuObserver: SignallingRef[IO, Option[Resource[IO, HtmlElement[IO]]]],
    site: Site,
    highlightStyle: SignallingRef[IO, String]
  ): Resource[IO, HtmlElement[IO]] = {
    for {
      styleDropDownOpen <- Resource.eval(SignallingRef.of[IO, Boolean](false))
      styleSearch       <- Resource.eval(SignallingRef.of[IO, String](""))
      searchInput       <- input.withSelf { searchInput =>
                             (
                               cls         := List(
                                 "appearance-none block w-full px-3 py-2 rounded-md text-gray-900 border border-gray-300 placeholder-gray-400 focus:outline-none focus:ring-1 focus:ring-blue-500 focus:border-blue-500 transition duration-150 ease-in-out"
                               ),
                               placeholder := "search...",
                               onChange --> {
                                 _.foreach { e =>
                                   searchInput.value.get.flatMap(styleSearch.set)
                                 }
                               }
                             )
                           }
      render            <- div(
                             cls := "flex bg-gray-900 text-white py-4 px-8 items-center space-x-8",
                             div(
                               cls := "flex-shrink-0 -my-4 -mx-4",
                               SvgIcons.logo("w-10 h-10")
//                               img(
//                                 src := site.thisVersionHref("/images/logo.svg"),
//                                 cls := "w-10 h-10"
//                               )
                             ),
                             navTag(
                               cls := "flex flex-1 md:flex-none space-x-4 items-center justify-start",
                               span(
                                 site.modules.take(1).map(moduleLink(page, site))
                               ),
                               span(
                                 cls := "text-gray-500 text-xs font-black",
                                 site.frontrouteVersion
                               )
                             ),
                             navTag(
                               cls := "hidden md:flex flex-1 space-x-4",
                               div(
                                 cls := "flex flex-wrap justify-start items-center space-x-2",
                                 site.modules.drop(1).map(moduleLink(page, site))
                               )
                             ),
                             div(
                               cls := "hidden lg:block",
                               img(src := "https://img.shields.io/maven-central/v/io.frontroute/frontroute-calico_sjs1_2.13.svg?versionPrefix=0.17", alt := "latest version on maven central")
                             ),
                             div(
                               cls := "hidden lg:block relative inline-block text-left",
                               div(
                                 button(
                                   `type`        := "button",
                                   cls           := "btn-sm-text-white",
                                   aria.hasPopup := true,
                                   aria.expanded <-- styleDropDownOpen,
                                   onClick --> {
                                     _.foreach { _ =>
                                       styleDropDownOpen.get.map(!_).flatMap(styleDropDownOpen.set)
                                     }
                                   },
                                   SvgIcons.paintbrushPencil("w-4 h-4 fill-current"),
//                                   Icons
//                                     .chevronDown(
//                                       svg.cls := "-mr-1 ml-2 h-4 fill-current text-gray-300"
//                                     ).hiddenIf(styleDropDownOpen.signal),
//                                   Icons
//                                     .chevronUp(
//                                       svg.cls := "-mr-1 ml-2 h-4 fill-current text-gray-300"
//                                     ).visibleIf(styleDropDownOpen.signal)
                                 )
                               ),
                               div(
//                                 TW.transition(styleDropDownOpen.signal),
                                 cls <-- (styleDropDownOpen: Signal[IO, Boolean]).ifF(
                                   List("origin-top-right absolute max-h-128 overflow-auto right-0 mt-2 w-56 rounded-md shadow-lg bg-white ring-1 ring-black ring-opacity-5 z-20 p-2"),
                                   List("hidden")
                                 ),
                                 div(
                                   cls              := List("py-1"),
                                   role             := List("menu"),
                                   aria.orientation := "vertical",
                                   aria.labelledBy  := "options-menu",
                                   div(
                                     cls := "mb-2",
                                     searchInput
                                   ),
                                   Styles.styles.map { styleName =>
                                     button(
                                       cls <-- styleSearch
                                         .map(search => !styleName.contains(search)).ifF(
                                           List("hidden"),
                                           List("block flex items-center space-x-2 w-full px-4 py-2 text-left text-gray-700 hover:bg-gray-200 hover:text-gray-900")
                                         ),
                                       onClick --> {
                                         _.foreach { _ =>
                                           highlightStyle.set(styleName)
                                         }
                                       },
                                       role := List("menuitem"),
                                       span(
                                         cls := List("flex-1"),
                                         styleName
                                       ),
                                       highlightStyle
                                         .map(_ == styleName)
                                         .map { b =>
                                           Option
                                             .when(b)(
                                               SvgIcons.squareCheck("w-4 h-4 fill-current")
                                             )
                                         },
                                     )
                                   }
                                 )
                               )
                             ),
                             //  div(
                             //    cls := "lg:hidden",
                             //    button(
                             //      "Menu",
                             //      cls := "btn-md-outline-white",
                             //      onClick.mapTo(
                             //        Some(
                             //          ModalContent(
                             //            div(
                             //              div(
                             //                cls := "flex justify-end py-4 px-8",
                             //                button(
                             //                  "Close",
                             //                  cls := "btn-md-outline-white",
                             //                  onClick.mapTo(None) --> menuObserver
                             //                )
                             //              ),
                             //              PageNavigation(page, mobile = true),
                             //              div(
                             //                cls := "flex flex-wrap justify-start items-center p-4",
                             //                Site.modules.drop(1).map(moduleLink(page))
                             //              )
                             //            ),
                             //            Some(menuObserver.contramap(_ => None))
                             //          )
                             //        )
                             //      ) --> menuObserver
                             //    )
                             //  ),
                             div(
                               cls := "hidden lg:block",
                               a(
                                 href := "https://github.com/tulz-app/frontroute-calico",
                                 SvgIcons.githubMarkWhite("w-6 h-6 fill-current")
                                 //  rel  := "external",
                               )
                             )
                           )
    } yield render
  }

  private def moduleLink(
    currentPage: Signal[IO, Option[(SiteModule, Page)]],
    site: Site,
  )(module: SiteModule) =
    a(
      cls  := "border-b-2 px-2 border-transparent flex font-display tracking-wide",
      cls <-- currentPage
        .map(_.exists(_._1.path == module.path))
        .ifF(
          List("border-gray-300 text-white"),
          List("text-gray-300 hover:border-gray-300 hover:text-white")
        ),
      href := site.thisVersionHref(s"/${module.path}"),
      module.title
    )

}
