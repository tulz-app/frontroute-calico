package frontroute.site.examples.ex_extract_matched_path

import frontroute.site.examples.CodeExample
import com.yurique.embedded.FileAsString

object ExtractMatchedPathExample
    extends CodeExample(
      id = "extract-matched-path",
      title = "Extract Matched Path",
      description = FileAsString("description.md"),
      links = Seq(
        "/",
        "/tabs/tab-1",
        "/tabs/tab-2",
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

      def ShowCurrentPath(label: String): Resource[IO, HtmlElement[IO]] =
        div(
          span(
            cls := "bg-yellow-200 text-yellow-900 rounded-sm space-x-2 text-sm px-2 font-mono",
            span(label),
            /* <focus> */
            extractMatchedPath.signal { path =>
              span(
                path.map(s => s"'${s.mkString("/", "/", "")}'")
              )
            }
            /* </focus> */
          )
        )

      def MyComponent(): Resource[IO, HtmlElement[IO]] =
        div(
          cls := "space-y-2",
          path(segment).signal { tab =>
            div(
              cls := "flex space-x-2",
              a(
                href := "tab-1",
                cls <-- tab
                  .map(_ == "tab-1").ifF(
                    List("text-xl px-4 py-1 rounded border-b-2 border-blue-800 bg-blue-200 text-blue-800"),
                    List("text-xl px-4 py-1 rounded border-b-2 border-transparent text-blue-700")
                  ),
                "Tab 1",
              ),
              a(
                href := "tab-2",
                cls <-- tab
                  .map(_ == "tab-2").ifF(
                    List("text-xl px-4 py-1 rounded border-b-2 border-blue-800 bg-blue-200 text-blue-800"),
                    List("text-xl px-4 py-1 rounded border-b-2 border-transparent text-blue-700")
                  ),
                "Tab 2",
              )
            )
          },
          div(
            ShowCurrentPath("Inside component:"),
            path("tab-1") {
              div(
                cls := "bg-blue-100 text-blue-600 p-4",
                div("Content one."),
                ShowCurrentPath("Inside tab-1:"),
              )
            },
            path("tab-2") {
              div(
                cls := "bg-blue-100 text-blue-600 p-4",
                div("Content two"),
                ShowCurrentPath("Inside tab-2:"),
              )
            },
          )
        )

      routes(
        div(
          cls := "p-4 min-h-[300px]",
          pathEnd {
            div(
              cls := "text-2xl",
              div(
                "Index page."
              ),
              ShowCurrentPath("Inside index:")
            )
          },
          pathPrefix("tabs") {
            div(
              MyComponent()
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
