package io.frontroute.site
package examples
package ex_basic

import io.frontroute.site.examples.CodeExample
import com.yurique.embedded.FileAsString

object BasicRoutingExample
    extends CodeExample(
      id = "basic-routing",
      title = "Basic routing",
      description = FileAsString("description.md"),
      links = Seq(
        "/",
        "/new-path",
        "/legacy-path",
        "/some-section/some-page",
        "/some-section/another-page"
      )
    )(() => {
      /* <focus> */
      import io.frontroute.*
      import io.frontroute.given
      /* </focus> */
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

      routes(
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
          (path("new-path") | path("legacy-path")) {
            /* </focus> */
            div(cls := "text-2xl", "new-path OR legacy-path")
            /* <focus> */
          },
          /* </focus> */
          /* <focus> */
          pathPrefix("some-section") {
            /* </focus> */
            /* <focus> */
            firstMatch(
              /* </focus> */
              /* <focus> */
              path("some-page") {
                /* </focus> */
                div(cls := "text-2xl", "Some page.")
                /* <focus> */
              },
              /* </focus> */
              /* <focus> */
              path("another-page") {
                /* </focus> */
                div(cls := "text-2xl", "Another page.")
                /* <focus> */
              }
              /* </focus> */
            )
            /* <focus> */
          },
          /* </focus> */
          /* <focus> */
          (noneMatched & extractUnmatchedPath) { unmatched =>
            /* </focus> */
            div(
              div(cls := "text-2xl", "Not Found"),
              div(
                cls   := "flex items-center space-x-2",
                span("Not found path:"),
                span(unmatched.mkString("/", "/", ""))
              )
            )
            /* <focus> */
          }
          /* </focus> */
        )
      )
    })
