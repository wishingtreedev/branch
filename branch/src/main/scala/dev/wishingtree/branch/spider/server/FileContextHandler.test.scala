package dev.wishingtree.branch.spider.server

import dev.wishingtree.branch.spider.HttpMethod
import dev.wishingtree.branch.macaroni.fs.PathOps.*
import dev.wishingtree.branch.testkit.fixtures.FileFixtureSuite

import java.nio.file.Files

class FileContextHandlerSpec extends FileFixtureSuite {

  tmpDir.test("FileContextHandler") { tmpDir =>

    Files.createDirectory(tmpDir / "css")
    Files.createDirectory(tmpDir / "blog")
    Files.createFile(tmpDir / "index.html")
    Files.createFile(tmpDir / "css" / "general.css")
    Files.createFile(tmpDir / "blog" / "post1.html")

    val files = FileContextHandler(tmpDir)

    assert(files.contextRouter.isDefinedAt((HttpMethod.GET, >> / "index.html")))
    assert(files.contextRouter.isDefinedAt((HttpMethod.GET, >>)))
    assert(
      files.contextRouter.isDefinedAt(
        (HttpMethod.GET, >> / "css" / "general.css")
      )
    )
    assert(
      files.contextRouter.isDefinedAt(
        (HttpMethod.GET, >> / "blog" / "post1")
      )
    )
  }

  fileWithSuffix(".html").test("FileContextHandler.defaultExists .html") {
    file =>
      val parent     = file.getParent
      val fileName   = file.relativeTo(parent).toString.stripSuffix(".html")
      val ctxHandler = FileContextHandler(parent)
      assert(ctxHandler.defaultExists(parent / fileName))
  }

  fileWithSuffix(".html").test("FileContextHandler.defaultFile .html") { file =>
    val parent     = file.getParent
    val fileName   = file.relativeTo(parent).toString.stripSuffix(".html")
    val ctxHandler = FileContextHandler(parent)
    assert(ctxHandler.defaultFile(parent / fileName).exists())
  }

  fileWithSuffix(".htm").test("FileContextHandler.defaultExists .htm") { file =>
    val parent     = file.getParent
    val fileName   = file.relativeTo(parent).toString.stripSuffix(".htm")
    val ctxHandler = FileContextHandler(parent)
    assert(ctxHandler.defaultExists(parent / fileName))
  }

  fileWithSuffix(".htm").test("FileContextHandler.defaultFile .htm") { file =>
    val parent     = file.getParent
    val fileName   = file.relativeTo(parent).toString.stripSuffix(".htm")
    val ctxHandler = FileContextHandler(parent)
    assert(ctxHandler.defaultFile(parent / fileName).exists())
  }

  fileWithSuffix(".txt").test("FileContextHandler.defaultExists .txt") { file =>
    val parent     = file.getParent
    val fileName   = file.relativeTo(parent).toString.stripSuffix(".txt")
    val ctxHandler = FileContextHandler(parent)
    assert(!ctxHandler.defaultExists(parent / fileName))
  }

  fileWithSuffix(".txt").test("FileContextHandler.defaultFile .txt") { file =>
    val parent     = file.getParent
    val fileName   = file.relativeTo(parent).toString.stripSuffix(".txt")
    val ctxHandler = FileContextHandler(parent)
    intercept[IllegalArgumentException] {
      ctxHandler.defaultFile(parent / fileName)
    }
  }
}
