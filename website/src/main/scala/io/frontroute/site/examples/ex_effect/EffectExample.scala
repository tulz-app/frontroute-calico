package io.frontroute.site.examples.ex_effect

import io.frontroute.site.examples.CodeExample
import com.yurique.embedded.FileAsString

object EffectExample
    extends CodeExample(
      id = "run-effect",
      title = "Run Effect",
      description = FileAsString("description.md"),
      links = Seq(
        "/",
        "/effect-1",
        "/effect-2",
        "/some-page"
      )
    )(() => {
      import io.frontroute.*
      import io.frontroute.given

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

      import org.scalajs.dom

      routes(
        div(
          cls := "p-4 min-h-[300px]",
          /* <focus> */
          pathEnd {
            /* </focus> */
            div(cls := "text-2xl", "Index page.")
            /* <focus> */
          },
          path("effect-1") {
            runEffect {
              IO {
                dom.console.log("effect 1")
              }
            }
          },
          path("effect-2") {
            runEffect {
              IO {
                dom.console.log("effect 2")
              }
            }
          },
          /* </focus> */
          (noneMatched & extractUnmatchedPath) { unmatched =>
            div(
              div(cls := "text-2xl", "Not Found"),
              div(unmatched.mkString("/", "/", ""))
            )
          }
        )
      )
    })
