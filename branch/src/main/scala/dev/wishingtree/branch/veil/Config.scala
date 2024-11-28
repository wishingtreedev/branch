package dev.wishingtree.branch.veil

import dev.wishingtree.branch.friday.JsonDecoder

import scala.util.*
import java.nio.file.{Files, Path}
import scala.io.Source

trait Config[T] {
  def fromFile(path: Path): Try[T]
  def fromFile(file: String): Try[T] = fromFile(Path.of(file))
  def fromResource(path: String): Try[T]
}

object Config {

  inline given derived[T](using JsonDecoder[T]): Config[T] =
    new Config[T] {

      override def fromFile(path: Path): Try[T] =
        Try(Files.readString(path))
          .flatMap(json => summon[JsonDecoder[T]].decode(json))

      override def fromResource(path: String): Try[T] = {
        scala.util
          .Using(Source.fromResource(path)) { iter =>
            val json = iter.mkString
            summon[JsonDecoder[T]].decode(json)
          }
          .flatten
      }
    }
}
