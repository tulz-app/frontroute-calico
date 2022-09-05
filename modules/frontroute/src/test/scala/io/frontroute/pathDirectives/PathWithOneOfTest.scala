package io.frontroute.pathDirectives

import io.frontroute.testing.TestBase
import io.frontroute._

class PathWithOneOfTest extends TestBase {

  test("path with oneOf path matcher") {
    routeTest(
      route = probe =>
        path(segment(Set("a", "b"))) { str =>
          testComplete {
            probe.append(str)
          }
        },
      init = locationProvider => {
        locationProvider.path("a")
        locationProvider.path("b")
      }
    ) { probe =>
      probe.toList shouldBe List("a", "b")
    }
  }

}
