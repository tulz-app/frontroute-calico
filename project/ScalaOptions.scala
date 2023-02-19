import sbt._

import sbt.Keys._

object ScalaOptions {

  val fixOptions = Seq(
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {

      case Some((3, _)) =>
        Seq(
          "-deprecation",
          "-feature",
          "-unchecked",
          "-language:higherKinds",
          "-language:implicitConversions",
          "-Ykind-projector",
          "-Xfatal-warnings"
        )

      case _ => throw new RuntimeException(s"unexpected scalaVersion: ${scalaVersion.value}")

    })
  )

}
