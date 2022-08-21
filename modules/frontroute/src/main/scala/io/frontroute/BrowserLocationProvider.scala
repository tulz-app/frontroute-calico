package io.frontroute

import com.raquo.airstream.core.EventStream
import org.scalajs.dom

import scala.scalajs.js

class BrowserLocationProvider(
  popStateEvents: EventStream[dom.PopStateEvent]
) extends LocationProvider {

  private var _current          = Option(Location(dom.window.location, js.undefined))
  def current: Option[Location] = _current

  val changes: EventStream[Unit] =
    popStateEvents.map { event =>
      _current = Some(Location(dom.window.location, event.state))
    }

}
