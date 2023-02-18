package io.frontroute

import cats.syntax.all._
import io.frontroute.domext.WindowWithScrollXY
import io.frontroute.internal.FrontrouteHistoryState
import io.frontroute.internal.HistoryState
import io.frontroute.internal.HistoryStateScrollPosition
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.Dynamic
import cats.effect.IO

object BrowserNavigation {

  private val windowWithScrollXY = dom.window.asInstanceOf[WindowWithScrollXY]

  private var preserveScroll = true

  def preserveScroll(keep: Boolean): Unit = {
    this.preserveScroll = keep
  }

  private def currentScrollPosition(): HistoryStateScrollPosition = {
    new HistoryStateScrollPosition(
      scrollX = windowWithScrollXY.scrollX,
      scrollY = windowWithScrollXY.scrollY
    )
  }

  private def createHistoryState(
    user: js.UndefOr[js.Any],
    saveCurrentScrollPosition: Boolean
  ): HistoryState = {
    val internal = new FrontrouteHistoryState(
      scroll = if (saveCurrentScrollPosition) {
        currentScrollPosition()
      } else {
        js.undefined
      }
    )

    new HistoryState(internal = internal, user = user)
  }

  def pushState(
    data: js.Any = js.undefined,
    url: js.UndefOr[String] = js.undefined,
    popStateEvent: Boolean = true
  ): IO[Unit] = {
    Frontroute.history.state.get.flatMap { historyState =>
      IO.whenA(preserveScroll) {
        val newState = HistoryState.tryParse(historyState) match {
          case Some(currentState) =>
            createHistoryState(
              user = currentState.user,
              saveCurrentScrollPosition = true
            )
          case None               =>
            createHistoryState(
              user = js.undefined,
              saveCurrentScrollPosition = true
            )
        }
        Frontroute.history.replaceState(
          newState
        )
//        dom.window.history.replaceState(
//          statedata = newState,
//          title = ""
//        )
      }.flatMap { _ =>
          val state = createHistoryState(
            user = data,
            saveCurrentScrollPosition = false
          )
          val eff   = url.toOption match {
            case Some(url) =>
              Frontroute.history.pushState(
                state = state,
                url = url
              )
            case None      =>
              Frontroute.history.pushState(
                state = state,
              )
          }
          eff >> IO.whenA(popStateEvent) { emitPopStateEvent(state) }
        }
    }

  }

  def replaceState(
    url: js.UndefOr[String] = js.undefined,
    data: js.Any = js.undefined,
    popStateEvent: Boolean = true
  ): IO[Unit] = {
    val state = createHistoryState(
      user = data,
      saveCurrentScrollPosition = false
    )
    val eff   = url.toOption match {
      case Some(url) =>
        Frontroute.history.replaceState(
          state = state,
          url = url
        )
      case None      =>
        Frontroute.history.replaceState(
          state = state,
        )
    }
    eff >> IO.whenA(popStateEvent) { emitPopStateEvent(state) }
  }

  def emitPopStateEvent(state: js.Any = js.undefined): IO[Unit] = IO {
    val event = js.Dynamic.newInstance(js.Dynamic.global.Event)("popstate").asInstanceOf[Dynamic]
    event.state = state
    val _     = dom.window.dispatchEvent(event.asInstanceOf[dom.PopStateEvent])
  }

  def restoreScroll(): Unit = {
    if (preserveScroll) {
      HistoryState
        .tryParse(dom.window.history.state.asInstanceOf[js.UndefOr[js.Any]].toOption)
        .flatMap(_.internal.toOption)
        .flatMap(_.scroll.toOption)
        .foreach { scroll =>
          dom.window.scrollTo(scroll.scrollX.fold(0)(_.round.toInt), scroll.scrollY.fold(0)(_.round.toInt))
        }
    }
  }

}
