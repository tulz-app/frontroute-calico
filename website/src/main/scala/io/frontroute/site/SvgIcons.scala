package io.frontroute.site

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

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
import org.scalajs.dom

object SvgIcons {

  @js.native
  @JSImport("svg/logo.svg?raw", JSImport.Default)
  private object _logo extends js.Any

  @js.native
  @JSImport("svg/github-mark-white.svg?raw", JSImport.Default)
  private object _githubMarkWhite extends js.Any

  @js.native
  @JSImport("svg/paintbrush-pencil-regular.svg?raw", JSImport.Default)
  private object _paintbrushPencil extends js.Any

  @js.native
  @JSImport("svg/square-check-regular.svg?raw", JSImport.Default)
  private object _squareCheck extends js.Any

  private case class Svg(src: String)

  private object Svg:

    given [N <: HtmlElement[IO]]: Modifier[IO, N, Svg] = (m, e) =>
      Resource.eval {
        IO {
          e.asInstanceOf[dom.HTMLElement].innerHTML = m.src
            .replace(
              """xmlns="http://www.w3.org/2000/svg"""",
              ""
            )
        }
      }

  private def svg(src: String)(cls: String) = span(
    styleAttr := "display: contents",
    Svg(src),
  ).evalTap { e =>
    IO {
      val node = e.asInstanceOf[dom.HTMLSpanElement]
      cls.split(' ').foreach { s =>
        node.children.item(0).classList.add(s.trim)
      }
    }
  }

  def logo(cls: String): Resource[IO, HtmlSpanElement[IO]]             = svg(_logo.asInstanceOf[String])(cls)
  def githubMarkWhite(cls: String): Resource[IO, HtmlSpanElement[IO]]  = svg(_githubMarkWhite.asInstanceOf[String])(cls)
  def paintbrushPencil(cls: String): Resource[IO, HtmlSpanElement[IO]] = svg(_paintbrushPencil.asInstanceOf[String])(cls)
  def squareCheck(cls: String): Resource[IO, HtmlSpanElement[IO]]      = svg(_squareCheck.asInstanceOf[String])(cls)

}
