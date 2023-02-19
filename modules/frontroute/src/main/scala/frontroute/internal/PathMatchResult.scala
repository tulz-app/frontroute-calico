package frontroute.internal

enum PathMatchResult[+A]:

  def map[B](f: A => B): PathMatchResult[B] = this match {
    case PathMatchResult.NoMatch                      => PathMatchResult.NoMatch
    case PathMatchResult.Rejected(tail)               => PathMatchResult.Rejected(tail)
    case PathMatchResult.Match(value, consumed, tail) => PathMatchResult.Match(f(value), consumed, tail)
  }

  case NoMatch extends PathMatchResult[Nothing]

  case Rejected[T](
    tail: List[String]
  ) extends PathMatchResult[T]

  case Match[T](
    value: T,
    consumed: List[String],
    tail: List[String]
  ) extends PathMatchResult[T]
