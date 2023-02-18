import sbt._

import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {

  val calico: Def.Initialize[Seq[ModuleID]] = Def.setting {
    Seq(
      "com.armanbilge" %%% "calico" % DependencyVersions.calico
    )
  }

  val tuplez: Def.Initialize[Seq[ModuleID]] = Def.setting {
    Seq(
      "app.tulz" %%% "tuplez-full-light" % DependencyVersions.tuplez
    )
  }

  val `tuplez-apply`: Def.Initialize[Seq[ModuleID]] = Def.setting {
    Seq(
      "app.tulz" %%% "tuplez-apply" % DependencyVersions.tuplez
    )
  }

  val domtestutils: Def.Initialize[Seq[ModuleID]] = Def.setting {
    Seq(
//      "com.raquo" %%% "domtestutils" % DependencyVersions.domtestutils % Test
    )
  }

  val scalatest: Def.Initialize[Seq[ModuleID]] = Def.setting {
    Seq(
      "org.scalatest" %%% "scalatest" % DependencyVersions.scalatest % Test,
//      ("org.scala-js" %%% "scalajs-java-securerandom" % DependencyVersions.`scalajs-java-securerandom` % Test).cross(CrossVersion.for3Use2_13)
    )
  }

  // wesite

  val `embedded-files-macro`: Def.Initialize[Seq[ModuleID]] = Def.setting {
    Seq(
      "com.yurique" %%% "embedded-files-macro" % DependencyVersions.`embedded-files-macro`
    )
  }

  val sourcecode: Def.Initialize[Seq[ModuleID]] = Def.setting {
    Seq(
      "com.lihaoyi" %%% "sourcecode" % DependencyVersions.sourcecode
    )
  }

  val `scala-js-macrotask-executor`: Def.Initialize[Seq[ModuleID]] = Def.setting {
    Seq(
      "org.scala-js" %%% "scala-js-macrotask-executor" % DependencyVersions.`scala-js-macrotask-executor`
    )
  }

}
