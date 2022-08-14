package io.frontroute.site

import com.raquo.laminar.api.L._
import io.frontroute.site.layout.PageWrap
import io.laminext.syntax.tailwind._
import io.laminext.tailwind.modal.ModalContent
import io.laminext.tailwind.theme.Modal
import io.laminext.tailwind.theme.Theme
import org.scalajs.dom
import io.frontroute._

class Routes {

  private val mobileMenuContent = Var[Option[ModalContent]](None)

  private def modulePrefix: Directive[SiteModule] =
    pathPrefix(segment).flatMap { moduleName =>
      provide(Site.findModule(moduleName)).collect { case Some(module) =>
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

  private val mobileMenuModal: Modal = Theme.current.modal.customize(
    contentWrapTransition = _.customize(
      nonHidden = _ :+ "bg-gray-900"
    )
  )

  private val versionSegment = {
    regex("\\d+\\.\\d+\\.\\S+".r).map(_.source)
  }

  private val versionPrefix =
    pathPrefix("v" / versionSegment)

  private val thisVersionPrefix =
    versionPrefix.filter(_.toString.startsWith(Site.frontrouteVersion)).mapTo(())

  def start(): Unit = {
    val appContainer  = dom.document.querySelector("#app")
    val menuContainer = dom.document.querySelector("#menu-modal")

    appContainer.innerHTML = ""
    com.raquo.laminar.api.L.render(
      appContainer,
      div(
        cls := "contents",
        thisVersionPrefix(
          concat(
            (pathEnd.mapTo(Some((Site.indexModule, Site.indexModule.index))) |
              (modulePrefix & pathEnd).map(m => Some((m, m.index))) |
              moduleAndPagePrefix.map(moduleAndPage => Some(moduleAndPage))).signal { moduleAndPage =>
              PageWrap(moduleAndPage, mobileMenuContent.writer)
            },
            div("Not Found")
          )
        ),
        div("TODO: reload")
      )
    )
    com.raquo.laminar.api.L.render(menuContainer, TW.modal(mobileMenuContent.signal, mobileMenuModal))
    BrowserNavigation.emitPopStateEvent()
  }

}
