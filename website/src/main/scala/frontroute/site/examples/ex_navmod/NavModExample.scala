package frontroute.site.examples.ex_navmod

import frontroute.site.examples.CodeExample
import com.yurique.embedded.FileAsString

object NavModExample
    extends CodeExample(
      id = "navmod",
      title = "navMod",
      description = FileAsString("description.md"),
      links = Seq(
        "/",
        "/page-1",
        "/page-2",
        "/page-3",
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

      val links = List(
        "page-1" -> "Page 1",
        "page-2" -> "Page 2",
        "page-3" -> "Page 3",
      )

      routes(
        div(
          cls := "p-4 min-h-[300px]",
          div(
            cls := "flex space-x-4",
            links.map { case (path, pageTitle) =>
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
                pageTitle
              )
            }
          ),
          pathEnd {
            div(
              cls := "text-2xl",
              div(
                "Index page."
              ),
            )
          },
          path("page-1") {
            div(
              cls := "text-2xl",
              div(
                "Page 1."
              ),
            )
          },
          path("page-2") {
            div(
              cls := "text-2xl",
              div(
                "Page 2."
              ),
            )
          },
          path("page-3") {
            div(
              cls := "text-2xl",
              div(
                "Page 3."
              ),
            )
          },
          (noneMatched & extractUnmatchedPath) { unmatched =>
            div(
              div(cls := "text-2xl", "Not Found"),
              div(unmatched.mkString("/", "/", ""))
            )
          }
        )
      )
    })
