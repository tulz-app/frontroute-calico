package frontroute.site.components

import cats.syntax.all.*
import frontroute.*
import frontroute.given
import frontroute.internal.UrlString
import io.laminext.highlight.Highlight
import frontroute.site.examples.CodeExample
import frontroute.site.Site
import frontroute.site.Styles
import frontroute.site.TemplateVars
import org.scalajs.dom
import org.scalajs.dom.HTMLIFrameElement
import org.scalajs.dom.Location
import org.scalajs.dom.document
import org.scalajs.dom.html
import org.scalajs.dom.window
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

import scala.scalajs.js

object CodeExampleDisplay {

  private def fixIndentation(src: String): String = {
    val lines =
      src
        .split('\n')
        .drop(1)
        .dropRight(1)

    val minIndent =
      lines
        .filterNot(_.trim.isEmpty)
        .map(_.takeWhile(_.isWhitespace))
        .map(_.length)
        .minOption
        .getOrElse(0)

    lines.map(_.drop(minIndent)).mkString("\n")
  }

  def apply(
    example: CodeExample,
    site: Site,
    highlightStyle: Signal[IO, String]
  ): Resource[IO, HtmlDivElement[IO]] =
    Resource.eval(SignallingRef.of[IO, Boolean](true)).flatMap { highlightRelevant =>
      // storedBoolean("dim-context", initial = true)
      val hasContext = example.code.source.contains("/* <focus> */")

      val codeNode = (highlightRelevant: Boolean) => {
        val theCode = pre(
          cls := "w-full text-sm language-scala",
          fixIndentation {
            example.code.source
          }
            .replaceAll(
              "\n[ ]*/\\* <focus> \\*/[ ]*\n",
              "\n/* <focus> */"
            )
            .replaceAll(
              "\n[ ]*/\\* </focus> \\*/[ ]*\n",
              "\n/* </focus> */"
            )
        ).evalTap { e =>
          val node = e.asInstanceOf[dom.HTMLDivElement]

          IO { Highlight.highlightElement(node) } >>
            IO { hideFocusMarkers(node) } >>
            IO.whenA(hasContext) {
              IO {
                val _ = js.timers.setTimeout(100) {
                  val updatedNode = setOpacityRecursively(node, 0, highlightRelevant)
                  val _           = node.parentNode.replaceChild(updatedNode, node)
                }
              }
            }
        }

        div(
          theCode,
        )
      }

      val tabs = List(
        "live"   -> "Live Demo",
        "source" -> "Source Code",
      ) ++ List("description" -> "Description").filterNot(_ => example.description.trim.isEmpty)

      div(
        cls := "flex-1 flex flex-col space-y-4",
        div(
          cls := "flex space-x-4 items-center",
          h1(
            cls := "font-display text-xl font-bold text-gray-900 tracking-wide",
            example.title
          ),
          div(
            cls := "flex space-x-2",
            tabs.map { case (path, tabLabel) =>
              a(
                href := path,
                navMod { active =>
                  cls <-- active.ifF(
                    "px-2 rounded bg-gray-500 text-gray-100 font-semibold".split(' ').toList,
                    "px-2 rounded text-gray-700 font-semibold".split(' ').toList
                  )
                },
                tabLabel
              )
            }
          )
        ),
        path(Set("live", "source", "description")).signal { tab =>
          div(
            cls := "flex-1 flex flex-col space-y-2",
            div(
              cls <-- tab.map(_ != "source").ifF(List("hidden"), List("flex-1 flex flex-col space-y-2")),
              div(
                cls := "flex space-x-4 items-center",
                Option
                  .when(hasContext) {
                    label(
                      cls := "btn-sm-text-blue flex-shrink-0 flex space-x-1 items-center cursor-pointer",
                      input.withSelf((el: HtmlInputElement[IO]) =>
                        (
                          tpe := "checkbox",
                          checked <-- highlightRelevant,
                          onClick --> { _.foreach { _ => el.checked.get.flatMap(highlightRelevant.set) } }
                        )
                      ),
                      span(
                        "highlight relevant code"
                      )
                    )
                  }.toList
              ),
              div(
                cls := "flex-1 shadow relative overflow-x-auto",
                highlightRelevant.map(codeNode)
              )
            ),
            div(
              (
                cls <-- tab.map(_ != "live").ifF(List("hidden"), List("flex-1 flex flex-col")),
                iframe.withSelf { frame =>
                  (
                    cls := "flex-1",
                    onLoad --> {
                      _.foreach { _ =>
                        IO {
                          val f = frame.asInstanceOf[dom.HTMLIFrameElement]
                          f.style.height = (f.contentWindow.document.body.scrollHeight + 20).toString + "px"
                        }
                      }
                    },
                    src := site.thisVersionHref(s"/example-frame/${example.id}")
                  )
                }
              )
            ),
            div(
              cls <-- tab.map(_ != "description").ifF(List("hidden"), List("flex-1 flex flex-col prose max-w-none")),
            ).flatTap { e =>
              val node = e.asInstanceOf[dom.HTMLDivElement]
              Resource.eval {
                IO {
                  node.innerHTML = TemplateVars(example.description)
                } >> IO {
                  node.querySelectorAll("pre > code").foreach { codeElement =>
                    Highlight.highlightElement(codeElement)
                  }
                }
              }
            }
          )
        },
        (noneMatched & extractUnmatchedPath) { unmatched =>
          div(s"Not Found - $unmatched")
        }
      )
    }

  def frame(example: CodeExample) = {
    def pathAndSearch(url: Location): String =
      url.pathname + (if (url.search != null && url.search.nonEmpty) url.search else "")

    val currentUrl =
      Window[IO]
        .history[Unit]
        .state
        .map(_ => window.location.toString)
        .map { case UrlString(url) => pathAndSearch(url) }

    for {
      _        <- Resource.eval {
                    BrowserNavigation.replaceState(url = "/")
                  }
      urlInput <- input.withSelf { urlInput =>
                    (
                      value <-- currentUrl.map(path => "https://site.nowhere" + path),
                      tpe         := "text",
                      placeholder := "https://site.nowhere/path",
                      cls         := "flex-1",
                      onKeyDown.filter(_.key == "Enter") --> {
                        _.foreach { e =>
                          e.stopPropagation >> e.preventDefault >>
                            urlInput.value.get.flatMap { case UrlString(url) =>
                              BrowserNavigation.pushState(url = pathAndSearch(url))
                            }
                        }
                      }
                    )
                  }
      render   <- div(
                    cls := "border-4 border-dashed border-blue-400 bg-blue-300 text-blue-900 rounded-lg p-6",
                    LinkHandler,
                    div(
                      cls := "-mx-6 -mt-6 p-2 rounded-t-lg bg-blue-500 flex space-x-1",
                      urlInput,
                      button(
                        cls := "btn-md-outline-white",
                        "Go!",
                        onClick --> {
                          _.foreach { _ =>
                            urlInput.value.get.flatMap { case UrlString(url) =>
                              BrowserNavigation.pushState(url = pathAndSearch(url))
                            }
                          }
                        }
                      )
                    ),
                    example.code.value(),
                    div(
                      cls := "rounded-b-lg bg-blue-900 -mx-6 -mb-6 p-2",
                      div(
                        cls := "font-semibold text-xl text-blue-200",
                        "Navigation"
                      ),
                      div(
                        cls := "flex flex-col p-2",
                        example.links.toList.map { path =>
                          a(
                            cls  := "text-blue-300 hover:text-blue-100",
                            href := path,
                            s"➜ $path"
                          )
                        }
                      )
                    )
                  )
    } yield render
  }

  @scala.annotation.unused
  private def opaqueColor(color: String, opaque: Int, dim: Boolean): String = {
    if (opaque == 0 && dim) {
      if (color.startsWith("rgb(")) {
        color.replace("rgb(", "rgba(").replace(")", ", .5)")
      } else {
        color
      }
    } else {
      color
    }
  }

  private def setOpacityRecursively(
    element: dom.HTMLElement,
    opaque: Int,
    dim: Boolean
  ): dom.Node = {
    val elementColor = dom.window.getComputedStyle(element).color
    val newElement   = element.cloneNode(false).asInstanceOf[html.Element]

    var childrenOpaque = opaque
    val newChildNodes  = element.childNodes.flatMap { child =>
      if (child.nodeName == "#text") {
        val span = dom.document.createElement("span").asInstanceOf[html.Element]
        span.innerText = child.textContent
        span.style.color = opaqueColor(elementColor, childrenOpaque, dim)
        Some(span)
      } else {
        if (child.innerText.contains("<focus>")) {
          childrenOpaque += 1
          None
        } else if (child.innerText.contains("</focus>")) {
          childrenOpaque -= 1
          None
        } else {
          Some(setOpacityRecursively(child.asInstanceOf[html.Element], childrenOpaque, dim))
        }
      }
    }
    newChildNodes.foreach(newElement.appendChild)
    newElement
  }

  private def hideFocusMarkers(element: html.Element): Unit =
    element.childNodes.foreach { child =>
      if (child.nodeName != "#text") {
        if (child.innerText.contains("<focus>") || child.innerText.contains("</focus>")) {
          child.asInstanceOf[html.Element].style.display = "none"
        } else {
          hideFocusMarkers(child.asInstanceOf[html.Element])
        }
      }
    }

}
