package io.frontroute.site
package examples
package ex_path_matching

import com.yurique.embedded.FileAsString
import io.frontroute.site.examples.CodeExample

object PathMatchingExample
    extends CodeExample(
      id = "path-matching",
      title = "Path matching",
      description = FileAsString("description.md"),
      links = Seq(
        "/",
        "/user/1",
        "/user/2",
        "/user/not-long",
        "/page-1",
        "/page-2",
        "/page-3",
        "/page-4"
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

      div(
        div(
          cls := "p-4 min-h-[300px]",
          /* <focus> */
          pathEnd {
            /* </focus> */
            div(cls := "text-2xl", "Index page.")
            /* <focus> */
          },
          /* </focus> */
          /* <focus> */
          path("user" / long) { userId =>
            /* </focus> */
            div(div(cls := "text-2xl", "User page."), div(s"User ID: $userId"))
            /* <focus> */
          },
          /* </focus> */
          /* <focus> */
          path(Set("page-1", "page-2", "page-3")) { page =>
            /* </focus> */
            div(div(cls := "text-2xl", "Some page."), div(s"Page name: $page"))
            /* <focus> */
          },
          /* </focus> */
          /* <focus> */
          (noneMatched & extractUnmatchedPath) { unmatched =>
            /* </focus> */
            div(
              div(cls := "text-2xl", "Not Found"),
              div(unmatched.mkString("/", "/", ""))
            )
            /* <focus> */
          }
          /* </focus> */
        )
      )
    })
