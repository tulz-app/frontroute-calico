package frontroute

import app.tulz.tuplez.ApplyConverter
import app.tulz.tuplez.ApplyConverters
import calico.html.HtmlAttr
import calico.html.Modifier
import frontroute.internal.LocationState
import frontroute.internal.UrlString
import frontroute.ops.DirectiveOfOptionOps
import frontroute.internal.PathMatchResult
import org.scalajs.dom
import org.scalajs.dom.HTMLAnchorElement
import org.scalajs.dom.MutationObserver
import org.scalajs.dom.MutationObserverInit
import org.scalajs.dom.MutationRecord
import org.scalajs.dom.html
import calico.*
import calico.html.io.given
import calico.html.io.*
import fs2.dom.*
import calico.syntax.*
import cats.effect.OutcomeIO
import cats.effect.*
import cats.effect.std.Hotswap
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.*
import fs2.concurrent.*

import scala.annotation.targetName
import scala.util.chaining.*
import scala.scalajs.js
import scala.util.matching.Regex
import scala.util.matching.Regex.Match
import scala.util.NotGiven

def segment: PathMatcher[String] =
  (consumed: List[String], in: List[String]) =>
    in match {
      case head :: tail => PathMatchResult.Match(head, consumed.appended(head), tail)
      case Nil          => PathMatchResult.NoMatch
    }

def segment(oneOf: Seq[String]): PathMatcher[String] =
  (consumed: List[String], in: List[String]) =>
    in match {
      case head :: tail =>
        if (oneOf.contains(head)) {
          PathMatchResult.Match(head, consumed.appended(head), tail)
        } else {
          PathMatchResult.Rejected(tail)
        }
      case Nil          => PathMatchResult.NoMatch
    }

def segment(oneOf: Set[String]): PathMatcher[String] =
  (consumed: List[String], in: List[String]) =>
    in match {
      case head :: tail =>
        if (oneOf.contains(head)) {
          PathMatchResult.Match(head, consumed.appended(head), tail)
        } else {
          PathMatchResult.Rejected(tail)
        }
      case Nil          => PathMatchResult.NoMatch
    }

def segment(s: String): PathMatcher0 =
  (consumed: List[String], in: List[String]) =>
    in match {
      case head :: tail =>
        if (head == s) {
          PathMatchResult.Match((), consumed.appended(head), tail)
        } else {
          PathMatchResult.Rejected(tail)
        }
      case Nil          => PathMatchResult.NoMatch
    }

def regex(r: Regex): PathMatcher[Match] =
  segment
    .map(r.findFirstMatchIn)
    .collect { case Some(m) => m }

def long: PathMatcher[Long] = segment.tryParse(_.toLong)

def double: PathMatcher[Double] = segment.tryParse(_.toDouble)

implicit def stringToSegment(s: String): PathMatcher[Unit] = segment(s)

implicit def setToSegment(oneOf: Set[String]): PathMatcher[String] = segment(oneOf)

implicit def setToSegment(oneOf: Seq[String]): PathMatcher[String] = segment(oneOf)

implicit def regexToPathMatcher(r: Regex): PathMatcher[Match] = regex(r)
