package io.frontroute.pathDirectives

import io.frontroute.testing.TestBase
import io.frontroute._

class PathWithOneOfAndRecoverTest extends TestBase {

  test("path with oneOf path matcher and recover") {
    routeTest(
      route = probe =>
        path(segment(Set("a", "b")).recover("default")) { str =>
          testComplete {
            probe.append(str)
          }
        },
      init = locationProvider => {
        locationProvider.path("a")
        locationProvider.path("b")
        locationProvider.path("c")
      }
    ) { probe =>
      probe.toList shouldBe List("a", "b", "default")
    }
  }

}
