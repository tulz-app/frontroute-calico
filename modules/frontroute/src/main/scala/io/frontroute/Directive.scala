package io.frontroute

import io.frontroute.ops.DirectiveOfOptionOps
import fs2.concurrent.Signal
import cats.effect.IO
import fs2.concurrent.SignallingRef

class Directive[L](
  val tapply: (L => Route) => Route
) extends ((L => Route) => Route):
  self =>

  def apply(a: L => Route): Route = tapply(a)

  def flatMap[R](next: L => Directive[R]): Directive[R] = {
    Directive[R] { inner =>
      self.tapply { value => (location, previous, state) =>
        next(value).tapply(inner)(location, previous, state.enter)
      }
    }
  }

  def map[R](f: L => R): Directive[R] =
    Directive[R] { inner =>
      self.tapply { value => (location, previous, state) =>
        val mapped = f(value)
        inner(mapped)(location, previous, state.enterAndSet(mapped))
      }
    }

  def tap(body: L => Unit): Directive[L] =
    Directive[L] { inner =>
      self.tapply { value => (location, previous, state) =>
        body(value)
        inner(value)(location, previous, state.enterAndSet(value))
      }
    }

  def emap[R](f: L => Either[Any, R]): Directive[R] =
    this.flatMap { value =>
      f(value).fold(
        _ => reject,
        r => Directive.provide(r)
      )
    }

  def opt: Directive[Option[L]] =
    this.map(v => Option(v)) | Directive.provide(None)

  @inline def some: Directive[Option[L]] = map(Some(_))

  @inline def none[R]: Directive[Option[R]] = mapTo(Option.empty[R])

  @inline def mapTo[R](otherValue: => R): Directive[R] = map(_ => otherValue)

  def &(magnet: ConjunctionMagnet[L]): magnet.Out = magnet(this)

  def |(other: Directive[L]): Directive[L] =
    Directive[L] { inner => (location, previous, state) =>
      self
        .tapply { value => (location, previous, state) =>
          inner(value)(location, previous, state.leaveDisjunction)
        }(location, previous, state.enterDisjunction) match {
        case RouteResult.Matched(state, location, consumed, result) => RouteResult.Matched(state, location, consumed, result)
        case RouteResult.RunEffect(state, location, consumed, run)  => RouteResult.RunEffect(state, location, consumed, run)
        case RouteResult.Rejected                                   =>
          other.tapply { value => (location, previous, state) =>
            inner(value)(location, previous, state.leaveDisjunction)
          }(location, previous, state.enterDisjunction)
      }
    }

  def collect[R](f: PartialFunction[L, R]): Directive[R] =
    Directive[R] { inner =>
      self.tapply { value => (location, previous, state) =>
        if (f.isDefinedAt(value)) {
          val mapped = f(value)
          inner(mapped)(location, previous, state.enterAndSet(mapped))
        } else {
          rejected
        }
      }
    }

  def filter(predicate: L => Boolean): Directive[L] =
    Directive[L] { inner =>
      self.tapply { value => (location, previous, state) =>
        if (predicate(value)) {
          inner(value)(location, previous, state.enter)
        } else {
          rejected
        }
      }
    }

  // def signal: Directive[Signal[IO, L]] =
  //   new Directive[Signal[IO, L]]({ inner => (location, previous, state) =>
  //     this.tapply { value => (location, previous, state) =>
  //       val next = state.unsetValue().enter
  //       previous.getValue[SignallingRef[IO, L]](next.path.key) match {
  //         case None              =>
  //           val newVar = Var(value)
  //           inner(newVar.signal)(location, previous, next.setValue(newVar))
  //         case Some(existingVar) =>
  //           existingVar.set(value)
  //           inner(existingVar)(location, previous, next.setValue(existingVar))
  //       }
  //     }(location, previous, state)
  //   })

object Directive:

  def provide[L](value: L): Directive[L] =
    Directive { inner => (location, previous, state) =>
      inner(value)(location, previous, state.enterAndSet(value))
    }

  def apply[L](f: (L => Route) => Route): Directive[L] = {
    new Directive[L](inner =>
      (location, previous, state) =>
        f(value =>
          (location, previous, state) => {
            inner(value)(location, previous, state)
          }
        )(location, previous, state)
    )
  }

  implicit def directiveOfOptionSyntax[A](underlying: Directive[Option[A]]): DirectiveOfOptionOps[A] = new DirectiveOfOptionOps[A](underlying)
