package frontroute.site

object TemplateVars {

  private val vars = Seq(
    "frontrouteVersion" -> "0.17.0-M5",
    "calicoVersion"     -> "0.2-8797711-SNAPSHOT",
    "scalajsVersion"    -> "1.13.0",
    "scala3version"     -> "3.2.1",
  )

  def apply(s: String): String =
    vars.foldLeft(s) { case (acc, (varName, varValue)) =>
      acc.replace(s"{{${varName}}}", varValue)
    }

}
