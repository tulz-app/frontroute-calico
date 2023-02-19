`frontroute-calico` is a router library for [Scala.js](https://www.scala-js.org/) + [Calico](https://www.armanbilge.com/calico/) applications.

```scala
import frontroute.*
import frontroute.given
import calico.*
import calico.html.*
import calico.html.io.given
import calico.html.io.*
import fs2.dom.*
import calico.syntax.*

routes(
  div(
    pathEnd {
      div(
        "Home Page"
      )
    },
    path("sign-in") {
      div(
        "Sign-in Page"
      )
    },
    path("sign-up") {
      div(
        "Sign-up Page"
      )
    },
    (noneMatched & extractUnmatchedPath) { unmatched =>
      div(
        s"Not Found - ${unmatched.mkString("/", "/", "")}"
      )
    }
  )
)
```

See [getting started](/getting-started).

## Installation

### Prerequisites

* [Scala.js](https://www.scala-js.org/) `v{{scalajsVersion}}`+
* Scala {{scala3version}}+
* [Calico](https://www.armanbilge.com/calico/) {{calicoVersion}} (it will be added to your project's dependencies transitively)

### sbt

Add the [Scala.js](https://www.scala-js.org/) plugin to your `project/plugins.sbt` file.

```scala
addSbtPlugin("org.scala-js" % "sbt-scalajs"  % {{scalajsVersion}})
```

Enable the plugin and add the `frontroute` library to your `build.sbt` file:

```scala
enablePlugins(ScalaJSPlugin)

libraryDependencies ++= Seq(
  "io.frontroute" %%% "frontroute-calico" % "{{frontrouteVersion}}"
)
```

### Mill

```scala
import $ivy.`com.lihaoyi::mill-contrib-bloop:$MILL_VERSION`
import mill._
import mill.scalalib._
import mill.scalajslib._
import mill.scalajslib.api._

object counter extends ScalaJSModule {
    def scalaVersion   = "{{scala3version}}"
    def scalaJSVersion = "{{scalajsVersion}}"
    
    def ivyDeps = Agg(ivy"io.frontroute::frontroute-calico::{{frontrouteVersion}}")
    
    override def moduleKind = T(mill.scalajslib.api.ModuleKind.CommonJSModule)
}
```
