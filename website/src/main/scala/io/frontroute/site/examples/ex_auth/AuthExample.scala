package io.frontroute.site
package examples
package ex_auth

import io.frontroute.site.examples.CodeExample
import com.yurique.embedded.FileAsString

object AuthExample
    extends CodeExample(
      id = "auth",
      title = "Auth",
      description = FileAsString("description.md"),
      links = Seq(
        "/",
        "/private/profile"
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

      case class User(id: String)

      sealed trait AuthenticationEvent extends Product with Serializable
      object AuthenticationEvent {
        case object SignedOut               extends AuthenticationEvent
        case class SignedIn(userId: String) extends AuthenticationEvent
      }

      for {
        authenticationEvents <- Resource.eval {
                                  fs2.concurrent.Channel.bounded[IO, AuthenticationEvent](5)
                                }
        /* <focus> */
        authenticatedUser    <-
          authenticationEvents.stream
            .map {
              case AuthenticationEvent.SignedOut        => Option.empty
              case AuthenticationEvent.SignedIn(userId) => Some(User(userId))
            }.holdResource(Option.empty[User])
        /* </focus> */
        route                <- div(
                                  authenticatedUser.map { maybeUser =>
                                    div(
                                      firstMatch(
                                        pathEnd {
                                          div(
                                            div(cls := "text-2xl", "Index page."),
                                            div(s"Maybe user: $maybeUser")
                                          )
                                        },
                                        /* <focus> */
                                        provideOption(maybeUser) { user =>
                                          /* </focus> */
                                          pathPrefix("private") {
                                            path("profile") {
                                              div(
                                                div(cls := "text-2xl", "Profile page."),
                                                div(s"User: $user")
                                              )
                                            }
                                          }
                                          /* <focus> */
                                        },
                                        /* </focus> */
                                        extractUnmatchedPath { unmatched =>
                                          div(
                                            div(cls := "text-2xl", "Not Found"),
                                            div(unmatched.mkString("/", "/", ""))
                                          )
                                        }
                                      )
                                    )
                                  }
                                )
        render               <- routes(
                                  div(
                                    cls := "p-4 min-h-[300px]",
                                    route
                                  ),
                                  div(
                                    cls := "bg-blue-900 -mx-6 p-2 space-y-2",
                                    div(
                                      cls := "font-semibold text-xl text-blue-200",
                                      "Sign in a user (empty for log out):"
                                    ),
                                    div(
                                      input.withSelf(self =>
                                        (
                                          tpe         := "text",
                                          placeholder := "Input a user ID and hit enter...",
                                          onKeyDown.filter(_.key == "Enter") --> {
                                            _.foreach { e =>
                                              e.stopPropagation >> e.preventDefault >>
                                                self.value.get.flatMap { userId =>
                                                  if (userId.isEmpty) {
                                                    authenticationEvents.send(AuthenticationEvent.SignedOut).void
                                                  } else {
                                                    authenticationEvents.send(AuthenticationEvent.SignedIn(userId)).void
                                                  }
                                                }
                                            }
                                          }
                                        )
                                      )
                                    )
                                  )
                                )
      } yield render
    })
