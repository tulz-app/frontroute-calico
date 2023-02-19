package io.frontroute.site
package examples
package ex_params

import com.yurique.embedded.FileAsString
import io.frontroute.site.examples.CodeExample

object ParamsExample
    extends CodeExample(
      id = "query-parameters",
      title = "Query parameters",
      description = FileAsString("description.md"),
      links = Seq(
        "/",
        "/movie?id=2356777",
        "/movie?id=0306414",
        "/movies/search?director=cameron&year=1991",
        "/movies/search?director=cameron",
        "/movies/search?year=1991"
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
          pathEnd {
            div(cls := "text-2xl", "Index page.")
          },
          /* <focus> */
          (path("movie") & param("id")) { movieId =>
            /* </focus> */
            div(div(cls := "text-2xl", "Movie page."), div(s"Movie ID: $movieId"))
            /* <focus> */
          },
          /* </focus> */
          /* <focus> */
          (
            path("movies" / "search") &
              maybeParam("director") &
              maybeParam("year")
          ) { (maybeDirector, maybeYear) =>
            /* </focus> */
            div(div(cls := "text-2xl", "Movie search page."), div(s"Director: $maybeDirector"), div(s"Year: $maybeYear"))
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
    })
