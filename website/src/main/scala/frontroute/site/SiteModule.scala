package frontroute.site

class SiteModule private (
  val path: String,
  val title: String,
  val index: Page,
  val navigation: List[(String, List[Page])]
) {

  def findPage(path: String): Option[Page] = {
    navigation.flatMap(_._2).find(_.path == path)
  }

}

object SiteModule {

  def apply(
    path: String,
    title: String,
    index: Page,
    navigation: (String, List[Page])*
  ): SiteModule = new SiteModule(path, title, index, navigation.toList)

}
