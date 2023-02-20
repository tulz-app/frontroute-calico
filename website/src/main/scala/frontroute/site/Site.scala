package frontroute.site

import cats.effect.IO
import frontroute.site.pages.CodeExamplePage
import frontroute.site.pages.DocumentationPage
import com.yurique.embedded.FileAsString
import fs2.concurrent.Signal
import frontroute.site.examples.CodeExample

class Site(
  highlightStyle: Signal[IO, String]
) {

  val frontrouteVersion: String = "0.17.x-calico"

  val thisVersionPrefix = s"/v/$frontrouteVersion/"

  def thisVersionHref(href: String): String =
    s"${thisVersionPrefix}${href.dropWhile(_ == '/')}"

  private def examplePage(
    example: CodeExample
  ): Page = Page(example.id, example.id + "/live", example.title, CodeExamplePage(example, this, highlightStyle))

  private def docPage(
    path: String,
    title: String,
    markdown: String
  ): Page = Page(path, path, title, DocumentationPage(title, markdown, this))

  val indexModule: SiteModule =
    SiteModule(
      path = "",
      title = "frontroute",
      index = docPage("", "frontroute", FileAsString("/doc/index.md"))
    )

  val examples: List[CodeExample] =
    List(
      frontroute.site.examples.ex_basic.BasicRoutingExample,
      frontroute.site.examples.ex_path_matching.PathMatchingExample,
      frontroute.site.examples.ex_recursive_path_matching.RecursivePathMatchingExample,
      frontroute.site.examples.ex_params.ParamsExample,
      frontroute.site.examples.ex_advanced_params.AdvancedParamsExample,
      frontroute.site.examples.ex_custom_directives.CustomDirectivesExample,
      frontroute.site.examples.ex_auth.AuthExample,
      frontroute.site.examples.ex_tabs.TabsExample,
      frontroute.site.examples.ex_nested.NestedExample,
      frontroute.site.examples.ex_effect.EffectExample,
      frontroute.site.examples.ex_extract_matched_path.ExtractMatchedPathExample,
      frontroute.site.examples.ex_matched_path.MatchedPathExample,
    )

  val modules: List[SiteModule] = List(
    indexModule,
    SiteModule(
      path = "getting-started",
      title = "Getting started",
      index = docPage("", "", FileAsString("/doc/getting-started/index.md")),
      ""                   -> List(
        docPage("first-routes", "First routes", FileAsString("/doc/getting-started/first-routes.md")),
        docPage("handling-not-found", "Handling 'Not Found'", FileAsString("/doc/getting-started/handling-not-found.md")),
        docPage("links-and-navigation", "Links and Navigation", FileAsString("/doc/getting-started/links-and-navigation.md")),
        docPage("building-routes", "Building routes", FileAsString("/doc/getting-started/building-routes.md")),
        docPage("nested-routes", "Nested routes", FileAsString("/doc/getting-started/nested-routes.md")),
      )
    ),
    SiteModule(
      path = "reference",
      title = "Reference",
      index = docPage("", "Reference", FileAsString("/doc/reference/index.md")),
      "Directives"         -> List(
        docPage("directives", "Built-in directives", FileAsString("/doc/reference/built-in-directives.md")),
        docPage("signal-directive", ".signal directive", FileAsString("/doc/reference/signal-directive.md")),
        docPage("conjunction", "conjunction (&)", FileAsString("/doc/reference/conjunction.md")),
        docPage("disjunction", "disjunction (|)", FileAsString("/doc/reference/disjunction.md")),
        docPage("directive-combinators", "Directive combinators", FileAsString("/doc/reference/directive-combinators.md")),
      ),
      "Path Matchers"      -> List(
        docPage("path-matchers", "Built-in path matchers", FileAsString("/doc/reference/built-in-path-matchers.md")),
        docPage("path-matcher-combinators", "Path matcher combinators", FileAsString("/doc/reference/path-matcher-combinators.md"))
      ),
      "Alternative Routes" -> List(
        docPage("first-match", "firstMatch", FileAsString("/doc/reference/first-match.md"))
      ),
      "Utilities"          -> List(
        docPage("navigation", "Browser navigation", FileAsString("/doc/reference/navigation.md")),
      ),
      "Extending"          -> List(
        docPage("custom-directives", "Custom directives", FileAsString("/doc/reference/custom-directives.md"))
      ),
      "Under the hood"     -> List(
        docPage("route", "Route", FileAsString("/doc/reference/under-the-hood/route.md")),
        docPage("directive", "Directive", FileAsString("/doc/reference/under-the-hood/directive.md")),
        docPage("path-matching", "Path-matching", FileAsString("/doc/reference/under-the-hood/path-matching.md")),
      ),
    ),
    SiteModule(
      path = "examples",
      title = "Examples",
      index = docPage("", "Examples", FileAsString("/doc/examples/index.md")),
      ""                   -> examples.map(ex => examplePage(ex))
    )
  )

  def findModule(path: String): Option[SiteModule] =
    modules.find(_.path == path)

}
