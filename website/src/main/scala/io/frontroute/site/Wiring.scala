package io.frontroute.site

import org.scalajs.dom

class Wiring(
  val ssrContext: SsrContext,
  val routes: Routes
)

object Wiring {

  def apply(site: Site): Wiring = {
    new Wiring(
      ssrContext = SsrContext(
        ssr = dom.window.navigator.userAgent == "frontroute/ssr"
      ),
      routes = Routes(site)
    )
  }

}
