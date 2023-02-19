package io.frontroute.site.examples.ex_nested

import io.frontroute.site.examples.CodeExample
import com.yurique.embedded.FileAsString

object NestedExample
    extends CodeExample(
      id = "nested-routes",
      title = "Nested routes",
      description = FileAsString("description.md"),
      links = Seq(
        "/",
        "/tabs/tab-1",
        "/tabs/tab-2",
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

      val tabs = List(
        "tab-1" -> "Tab 1",
        "tab-2" -> "Tab 2",
      )

      def MyComponent(): Resource[IO, HtmlElement[IO]] =
        div(
          cls := "space-y-2",
          div(
            cls := "flex space-x-2",
            tabs.map { case (path, tabLabel) =>
              a(
                href := path,
                /* <focus> */
                navMod { active =>
                  cls <-- active.ifF(
                    List("text-xl px-4 py-1 rounded border-b-2 border-blue-800 bg-blue-200 text-blue-800"),
                    List("text-xl px-4 py-1 rounded border-b-2 border-transparent text-blue-700")
                  )
                },
                /* </focus> */
                tabLabel
              )
            }
          ),
          div(
            /* <focus> */
            path("tab-1") {
              /* </focus> */
              div("Content one.", cls := "bg-blue-100 text-blue-600 p-4")
              /* <focus> */
            },
            /* </focus> */
            /* <focus> */
            path("tab-2") {
              /* </focus> */
              div("Content two", cls := "bg-blue-100 text-blue-600 p-4")
              /* <focus> */
            }
            /* </focus> */
          )
        )

      div(
        div(
          cls := "p-4 min-h-[300px]",
          pathEnd {
            div(cls := "text-2xl", "Index page.")
          },
          /* <focus> */
          pathPrefix("tabs") {
            MyComponent()
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
