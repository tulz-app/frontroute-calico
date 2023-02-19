package frontroute.site
package examples
package ex_tabs

import frontroute.site.examples.CodeExample
import com.yurique.embedded.FileAsString

object TabsExample
    extends CodeExample(
      id = "tabs",
      title = "Tabs",
      description = FileAsString("description.md"),
      links = Seq(
        "/",
        "/tab-1",
        "/tab-2",
        "/some-page"
      )
    )(() => {
      import frontroute.*
      import frontroute.given

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

      routes(
        div(
          cls := "p-4 min-h-[300px]",
          div(
            cls := "space-y-2",
            div(
              cls := "flex space-x-2",
              tabs.map { case (path, tabLabel) =>
                a(
                  relativeHref(path),
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
            /* <focus> */
            path(Set("tab-1", "tab-2")).signal { tab =>
              /* </focus> */
              div(
                div(
                  cls <-- tab.map(_ != "tab-1").ifF(List("hidden"), List.empty),
                  textArea("tab-1 text area", cls := "bg-blue-100 text-blue-500")
                ),
                div(
                  cls <-- tab.map(_ != "tab-2").ifF(List("hidden"), List.empty),
                  textArea("tab-2 text area", cls := "bg-blue-100 text-blue-500")
                )
              )

              /* <focus> */
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
      )
    })
