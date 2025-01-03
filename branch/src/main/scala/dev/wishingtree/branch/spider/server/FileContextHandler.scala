package dev.wishingtree.branch.spider.server

import com.sun.net.httpserver.{Authenticator, Filter}
import dev.wishingtree.branch.spider.*
import dev.wishingtree.branch.macaroni.fs.PathOps.*

import java.io.File
import java.nio.file.{Files, Path}

object FileContextHandler {

  /** A list of default files to look for when a directory is requested. E.g.
    * /some/path -> /some/path/index.html
    */
  private[spider] val defaultFiles: List[String] = List(
    "index.html",
    "index.htm"
  )

  /** A list of default file endings to look for when a file is requested. E.g.
    * /some/path -> /some/path.html
    */
  private[spider] val defaultEndings: List[String] = List(
    ".html",
    ".htm"
  )

}

/** A built-in context handler for serving files from the file system.
  */
case class FileContextHandler(
    rootFilePath: Path,
    contextPath: String = "/",
    override val filters: Seq[Filter] = Seq.empty,
    override val authenticator: Option[Authenticator] = Option.empty
) extends ContextHandler(contextPath) {

  private def fileExists(path: Path): Boolean = {
    val filePath = (rootFilePath / path).toString
    val file     = new File(filePath)
    file.exists() && file.isFile
  }

  private[spider] def defaultExists(path: Path): Boolean = {
    if Files.isDirectory(rootFilePath / path) then {
      // If the path is a folder, see if a default file exists...
      FileContextHandler.defaultFiles.foldLeft(false) { (b, d) =>
        val file = new File((rootFilePath / path / d).toString)
        b || (file.exists() && file.isFile)
      }
    } else {
      // ... otherwise see if a file with a default ending exists
      FileContextHandler.defaultEndings.foldLeft(false) { (b, d) =>
        val file = new File((rootFilePath / path).toString + d)
        b || (file.exists() && file.isFile)
      }
    }

  }

  private[spider] def defaultFile(path: Path): File =
    FileContextHandler.defaultFiles.iterator
      .map(fn => new File((rootFilePath / path / fn).toString))
      .find(_.exists())
      .orElse(
        FileContextHandler.defaultEndings.iterator
          .map(suffix => new File((rootFilePath / path).toString + suffix))
          .find(_.exists())
      )
      .getOrElse(throw new IllegalArgumentException("Not found"))

  private val fileHandler: FileHandler =
    FileHandler(rootFilePath)

  override val contextRouter
      : PartialFunction[(HttpMethod, Path), RequestHandler[?, ?]] = {
    case HttpMethod.GET -> anyPath if fileExists(anyPath)    => fileHandler
    case HttpMethod.GET -> anyPath if defaultExists(anyPath) =>
      DefaultFileHandler(defaultFile(anyPath))
  }
}
