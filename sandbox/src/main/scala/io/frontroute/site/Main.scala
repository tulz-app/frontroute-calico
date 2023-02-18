import calico.*

import calico.html.io.given
import calico.html.io.*
import calico.syntax.*
import cats.effect.*
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.*
import fs2.concurrent.*
import io.frontroute.*
import io.frontroute.given

object Main extends IOWebApp {

  def Counter(label: String, initialStep: Int) =
    SignallingRef[IO].of(initialStep).product(Channel.unbounded[IO, Int]).toResource.flatMap { (step, diff) =>

      val allowedSteps = List(1, 2, 3, 5, 10)

      div(
        cls := "space-y-4 p-4",
        div(
          a(href := "/page-1", "page-1"),
          a(href := "/page-2", "page-2"),
          a(href := "/page-3", "page-3"),
        ),
        div(
          a(href := "/effect-1", "effect-1"),
          a(href := "/effect-2/sub-1", "effect-2/sub-1"),
          a(href := "/effect-2/sub-2", "effect-2/sub-2"),
          a(href := "/effect-2/sub-3", "effect-2/sub-3"),
        ),
        pathEnd {
          div("path ends here")
        },
        path("effect-1").execute {
          IO(println("effect-1"))
        },
        path("effect-2" / segment).execute { subSegment =>
          IO(println(s"effect-1/$subSegment"))
        },
        path(segment) { segment =>
          div(
            div(
              textArea("I should I re-render!..", rows := 5, cols := 40)
            ),
            div(
              span("Segment: "),
              span(segment)
            )
          )
        },
        path(segment).signal { (segment: Signal[IO, String]) =>
          div(
            div(
              textArea("will I re-render?..", rows := 5, cols := 40)
            ),
            div(
              span("Segment (signal): "),
              span(segment)
            )
          )
        },
        LinkHandler,
        p(
          cls := "flex items-center space-x-4",
          span("Step:"),
          select.withSelf { self =>
            (
              allowedSteps.map(step => option(value := step.toString, step.toString)),
              value <-- step.map(_.toString),
              onChange --> {
                _.evalMap(_ => self.value.get.map(_.toIntOption)).unNone.foreach(step.set)
              }
            )
          }
        ),
        p(
          cls := "flex items-center space-x-4",
          span(label + ": "),
          b(
            cls := "text-xl font-black",
            diff.stream.scanMonoid.map(_.toString).holdResource("0")
          ),
          button(
            cls := "px-2 bg-sky-200 rounded",
            "-",
            onClick --> {
              _.evalMap(_ => step.get).map(-1 * _).foreach(diff.send(_).void)
            }
          ),
          button(
            cls := "px-2 bg-sky-200 rounded",
            "+",
            onClick --> (_.evalMap(_ => step.get).foreach(diff.send(_).void))
          )
        )
      )
    }

  def render = div(
    cls := "p-4",
    h1(cls := "text-lg font-medium", "Let's count!"),
    Counter("Sheep", initialStep = 3)
  )

  override def rootElementId = "app-container"

}
