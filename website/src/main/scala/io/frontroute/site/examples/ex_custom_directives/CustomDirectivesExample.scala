package io.frontroute.site
package examples
package ex_custom_directives

import com.yurique.embedded.FileAsString
import io.frontroute.site.examples.CodeExample

object CustomDirectivesExample
    extends CodeExample(
      id = "custom-directives",
      title = "Custom directives",
      description = FileAsString("description.md"),
      links = Seq(
        "/",
        "/movie?id=1",
        "/movie?id=2",
        "/movie?id=not-long"
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
      
      import scala.util._

      def longParam(paramName: String): Directive[Long] =
        /* <focus> */
        param(paramName).flatMap { paramValue =>
          Try(paramValue.toLong).fold(
            _ => reject,
            parsed => provide(parsed)
          )
        }
      /* </focus> */

      routes(
        div(
          cls := "p-4 min-h-[300px]",
          pathEnd {
            div(cls := "text-2xl", "Index page.")
          },
          (
            path("movie") &
              /* <focus> */
              longParam("id")
            /* </focus> */
          ) { movieId =>
            div(div(cls := "text-2xl", "Movie page."), div(s"Movie ID (long): $movieId"))
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
