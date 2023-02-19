import org.scalajs.linker.interface.ESVersion

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.firefox.FirefoxProfile
import org.openqa.selenium.remote.server.DriverFactory
import org.openqa.selenium.remote.server.DriverProvider

import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import org.scalajs.jsenv.selenium.SeleniumJSEnv

import java.util.concurrent.TimeUnit

val disableWebsiteOnCI = false

val ciVariants = List("ciFirefox", "ciChrome", "ciJSDOMNodeJS")

lazy val useJSEnv =
  settingKey[JSEnv]("Use Node.js or a headless browser for running Scala.js tests")

addCommandAlias("ci", ciVariants.mkString("; ", "; ", ""))

addCommandAlias("ciFirefox", "; set Global / useJSEnv := JSEnv.Firefox; test; set Global / useJSEnv := JSEnv.NodeJS")
addCommandAlias("ciChrome", "; set Global / useJSEnv := JSEnv.Chrome; test; set Global / useJSEnv := JSEnv.NodeJS")
addCommandAlias("ciJSDOMNodeJS", "; set Global / useJSEnv := JSEnv.JSDOMNodeJS; test; set Global / useJSEnv := JSEnv.NodeJS")

Global / useJSEnv := JSEnv.NodeJS

inThisBuild(
  List(
    resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
    organization                               := "io.frontroute",
    homepage                                   := Some(url("https://github.com/tulz-app/frontroute-calico")),
    licenses                                   := List("MIT" -> url("https://github.com/tulz-app/frontroute-calico/blob/main/LICENSE.md")),
    scmInfo                                    := Some(ScmInfo(url("https://github.com/tulz-app/frontroute-calico"), "scm:git@github.com/tulz-app/frontroute.git")),
    developers                                 := List(Developer("yurique", "Iurii Malchenko", "i@yurique.com", url("https://github.com/yurique"))),
    description                                := "Router library for Calico with DSL inspired by Akka HTTP.",
    Test / publishArtifact                     := false,
    scalafmtOnCompile                          := false,
    versionScheme                              := Some("early-semver"),
    scalaVersion                               := ScalaVersions.v3,
    crossScalaVersions                         := Seq(
      ScalaVersions.v3,
    ),
    versionPolicyIntention                     := Compatibility.BinaryCompatible,
    githubWorkflowJavaVersions                 := Seq(JavaSpec.temurin("17")),
//    githubWorkflowBuild += WorkflowStep.Sbt(List("versionPolicyCheck")),
    githubWorkflowTargetTags ++= Seq("v*"),
    githubWorkflowArtifactUpload               := false,
    githubWorkflowPublishTargetBranches        := Seq(RefPredicate.StartsWith(Ref.Tag("v"))),
    githubWorkflowPublish                      := Seq(WorkflowStep.Sbt(List("ci-release"))),
    githubWorkflowBuild                        := Seq(
      WorkflowStep.Sbt(
        List("${{ matrix.ci }}")
      )
    ) ++ Seq(
      WorkflowStep.Sbt(
        List("website/fastLinkJS"),
        name = Some("build website"),
        cond = Some("matrix.ci == 'ciJSDOMNodeJS'")
      )
    ).filterNot(_ => disableWebsiteOnCI),
    githubWorkflowEnv ~= (_ ++ Map(
      "PGP_PASSPHRASE"    -> s"$${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET"        -> s"$${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> s"$${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> s"$${{ secrets.SONATYPE_USERNAME }}"
    )),
    githubWorkflowBuildPreamble ++= Seq(
      WorkflowStep.Use(
        UseRef.Public("actions", "setup-node", "v3"),
        name = Some("Setup NodeJS v18 LTS"),
        params = Map("node-version" -> "18", "cache" -> "npm"),
        cond = Some("matrix.ci == 'ciJSDOMNodeJS'"),
      ),
      WorkflowStep.Run(
        List("npm install"),
        name = Some("Install jsdom"),
        cond = Some("matrix.ci == 'ciJSDOMNodeJS'")
      ),
    ),
    githubWorkflowBuildMatrixAdditions += "ci" -> ciVariants,
    Test / jsEnv                               := {
      import JSEnv._

      val old = (Test / jsEnv).value

      useJSEnv.value match {
        case NodeJS      => old
        case JSDOMNodeJS => new JSDOMNodeJSEnv()
        case Firefox     =>
          val profile = new FirefoxProfile()
          profile.setPreference("privacy.file_unique_origin", false)
          val options = new FirefoxOptions()
          options.setProfile(profile)
          options.setHeadless(true)
          new SeleniumJSEnv(options)
        case Chrome      =>
          val options = new ChromeOptions()
          options.setHeadless(true)
          options.addArguments("--allow-file-access-from-files")
          val factory = new DriverFactory {
            val defaultFactory = SeleniumJSEnv.Config().driverFactory

            def newInstance(capabilities: org.openqa.selenium.Capabilities): WebDriver = {
              val driver = defaultFactory.newInstance(capabilities).asInstanceOf[ChromeDriver]
              driver.manage().timeouts().pageLoadTimeout(1, TimeUnit.HOURS)
              driver.manage().timeouts().setScriptTimeout(1, TimeUnit.HOURS)
              driver
            }

            def registerDriverProvider(provider: DriverProvider): Unit =
              defaultFactory.registerDriverProvider(provider)
          }
          new SeleniumJSEnv(options, SeleniumJSEnv.Config().withDriverFactory(factory))
      }
    }
  )
)

lazy val frontroute =
  project
    .in(file("modules/frontroute"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      name                     := "frontroute",
      libraryDependencies ++=
        Seq.concat(
          Dependencies.calico.value,
          Dependencies.`tuplez-apply`.value,
          Dependencies.tuplez.value,
          Dependencies.scalatest.value,
          Dependencies.`scala-js-macrotask-executor`.value.map(_ % Test)
        ),
      Test / parallelExecution := false,
      ScalaOptions.fixOptions,
      scalacOptions ++= {
        val sourcesGithubUrl  = s"https://raw.githubusercontent.com/tulz-app/frontroute/${git.gitHeadCommit.value.get}/"
        val sourcesOptionName = CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, _)) => "-P:scalajs:mapSourceURI"
          case Some((3, _)) => "-scalajs-mapSourceURI"
          case _            => throw new RuntimeException(s"unexpected scalaVersion: ${scalaVersion.value}")
        }
        val moduleSourceRoot  = file("").toURI.toString
        Seq(
          s"$sourcesOptionName:$moduleSourceRoot->$sourcesGithubUrl"
        )
      }
    )

lazy val website = project
  .in(file("website"))
  .enablePlugins(ScalaJSPlugin)
  .enablePlugins(EmbeddedFilesPlugin)
  .settings(ScalaOptions.fixOptions)
  .settings(noPublish)
  .settings(
    githubWorkflowTargetTags        := Seq.empty,
    publish / skip                  := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    scalaJSLinkerConfig ~= { _.withESFeatures(_.withESVersion(ESVersion.ES2015)) },
    Compile / scalaJSLinkerConfig ~= { _.withSourceMap(false) },
    scalaJSUseMainModuleInitializer := true,
    //    scalaJSLinkerConfig ~= (_.withModuleSplitStyle(org.scalajs.linker.interface.ModuleSplitStyle.FewestModules)),
    libraryDependencies ++= Seq.concat(
      Dependencies.calico.value,
      Dependencies.`embedded-files-macro`.value,
      Dependencies.sourcecode.value
    ),
    embedTextGlobs                  := Seq("**/*.md"),
    embedDirectories ++= (Compile / unmanagedSourceDirectories).value,
    (Compile / sourceGenerators) += embedFiles
  )
  .dependsOn(
    frontroute
  )

lazy val noPublish = Seq(
  publishLocal / skip := true,
  publish / skip      := true,
  publishTo           := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
)

lazy val root = project
  .in(file("."))
  .settings(noPublish)
  .settings(
    name := "frontroute-root"
  )
  .aggregate(
    frontroute
  )
