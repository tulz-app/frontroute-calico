package frontroute.internal

import cats.effect.IO
import fs2.dom.Serializer
import org.scalajs.dom

import scala.scalajs.js

private[frontroute] class HistoryStateScrollPosition(
  val scrollX: js.UndefOr[Double],
  val scrollY: js.UndefOr[Double]
) extends js.Object

private[frontroute] class FrontrouteHistoryState(
  val scroll: js.UndefOr[HistoryStateScrollPosition]
) extends js.Object

private[frontroute] class HistoryState(
  var internal: js.UndefOr[FrontrouteHistoryState],
  var user: js.UndefOr[js.Any],
  val frontroute: js.UndefOr[String] = "frontroute"
) extends js.Object {}

object HistoryState {

  def tryParse(raw: Option[js.UndefOr[js.Any]]): Option[HistoryState] = {
    raw
      .flatMap(_.toOption).fold(
        Some(new HistoryState(js.undefined, js.undefined))
      ) { raw =>
        val state = raw.asInstanceOf[HistoryState]
        if (state.frontroute.contains("frontroute")) {
          Some(state)
        } else {
          dom.console.debug("history state was set outside frontroute", state, state.isInstanceOf[HistoryState])
          None
        }
      }
  }

}
