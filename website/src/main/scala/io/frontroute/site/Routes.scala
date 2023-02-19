package io.frontroute.site

import cats.syntax.all.*
import io.frontroute.site.layout.PageWrap
import org.scalajs.dom
import io.frontroute.*
import io.frontroute.given
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

class Routes(
  site: Site
) {

  private def modulePrefix: Directive[SiteModule] =
    pathPrefix(segment).flatMap { moduleName =>
      provide(site.findModule(moduleName)).collect { case Some(module) =>
        module
      }
    }

  private def moduleAndPagePrefix: Directive[(SiteModule, Page)] =
    modulePrefix.flatMap { module =>
      pathPrefix(segment).flatMap { pageName =>
        provide(module.findPage(pageName)).collect { case Some(page) =>
          (module, page)
        }
      }
    }

//  private val mobileMenuModal: Modal = Theme.current.modal.customize(
//    contentWrapTransition = _.customize(
//      nonHidden = _ :+ "bg-gray-900"
//    )
//  )

  private val versionSegment = {
    regex("\\d+\\.\\d+\\.\\S+".r).map(_.source)
  }

  private val versionPrefix =
    pathPrefix("v" / segment)

  private val anyVersionPrefix =
    versionPrefix.mapTo(())

  private val thisVersionPrefix =
    versionPrefix.filter(_.toString.startsWith(site.frontrouteVersion)).mapTo(())

  def apply(
    highlightStyle: SignallingRef[IO, String],
  ): Resource[IO, HtmlElement[IO]] = {
    //  private val mobileMenuContent = Var[Option[ModalContent]](None)

    Resource
      .eval(
        SignallingRef[IO, Option[Resource[IO, HtmlElement[IO]]]](Option.empty),
      )
      .flatMap { mobileMenuContent =>
        div(
          cls := "contents",
          routes(
            div(
              LinkHandler,
              thisVersionPrefix {
                (
                  pathEnd.mapTo((site.indexModule, site.indexModule.index).some) |
                    (modulePrefix & pathEnd).map(m => (m, m.index).some) |
                    moduleAndPagePrefix.map(moduleAndPage => moduleAndPage.some)
                ).signal { moduleAndPage =>
                  PageWrap(moduleAndPage, mobileMenuContent, site, highlightStyle)
                }
              },
              (noneMatched & anyVersionPrefix) {
                div("Not Found - wrong version")
              },
              (noneMatched & extractUnmatchedPath) { unmatched =>
                div(s"Not Found! - $unmatched")
              }
            )
          )
        )
      }

//    val _ = com.raquo.laminar.api.L.render(menuContainer, TW.modal(mobileMenuContent.signal, mobileMenuModal))
  }

}
