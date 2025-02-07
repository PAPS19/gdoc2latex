
import java.io.File
import java.nio.charset.StandardCharsets
import scala.io.Source

/**
 * A context represents possibly other files needed to build the project, including the template into which the latex text is generated.
 *
 * A context always contains a template and may contain other files in a directory (no subdirectories).
 *
 * If no context id is provided, a default template will be loaded and no other files are in the context.
 *
 * If an id to a google docs file is provided, the plain text of that file is used as the template and no other files are in the context
 *
 * If an id to a google drive folder is provided, the files in that folder other than main.tex are included in the context. If a main.tex file is included that is used as the template, otherwise a default template is used.
 *
 * After rendering the context is turned into a LatexInput which contains the same files, but with an extra "main.tex" file that contains the rendered document (using the template)
 */
object Context {

  lazy val defaultTemplate: String =
    Source.fromInputStream(this.getClass.getResourceAsStream("/default_template.tex")).getLines().mkString("\n")

  def fromGoogleId(id: String): Context = {
    val dir = GDocConnection.getFileOrDirectory(id)
    val files = dir.filter(_.name != "main.tex").map(f => (f.name, f.content)).toMap
    val template = dir.find(_.name == "main.tex").map(f => new String(f.content, StandardCharsets.UTF_8)).getOrElse(defaultTemplate)

    Context(template, files)
  }

  def fromFile(file: File): Context = {
    val template = Source.fromFile(file).getLines().mkString("\n")
    Context(template, Map())
  }

  def defaultContext: Context = Context(defaultTemplate, Map())

}

case class Context(template: String, files: Map[String, Array[Byte]]) {
  def render(doc: LatexDoc): LatexInput = {
    val mainTex = template.replace("\\TITLE", doc.title)
      .replace("\\ABSTRACT", doc.abstr)
      .replace("\\CONTENT", doc.latexBody)
    val mainFile = ("main.tex" -> mainTex.getBytes(StandardCharsets.UTF_8))
    LatexInput(doc.title, files + mainFile)
  }
}

case class LatexInput(title: String, files: Map[String, Array[Byte]]) {

  val mainFile = "main.tex"

  def mainFileContent: Array[Byte] = files(mainFile)

  def mainFileString: String = new String(files(mainFile), StandardCharsets.UTF_8)
}
