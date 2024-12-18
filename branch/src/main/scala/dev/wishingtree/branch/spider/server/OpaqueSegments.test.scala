package dev.wishingtree.branch.spider.server

import dev.wishingtree.branch.spider.server.OpaqueSegments.*
import munit.FunSuite

class OpaqueSegmentsSpec extends FunSuite {

  test("appendStr") {
    assertEquals(
      >> / "a" / "b",
      Segments("a/b")
    )
  }

  test("appendPath") {
    assertEquals(
      Segments("a/b") / Segments("c"),
      Segments("a/b/c")
    )
  }

  test("string interpolation") {
    assertEquals(
      p"a/b",
      Segments("a/b")
    )
  }

  test("partial function matching") {

    val pf: PartialFunction[Segments, String] = {
      // `
      case >> / "a" / "b" / s"$arg" => arg
    }

    assert(pf(p"a/b/123") == "123")
    assert(!pf.isDefinedAt(p"a/b/123/xyz"))
    assert(!pf.isDefinedAt(p"a/b/"))

  }

  test("case insensitive matching") {

    val pf: PartialFunction[Segments, String] = {
      case >> / ci"this" / ci"that" / s"$arg" => arg
    }

    assert(pf(p"ThIs/tHaT/123") == "123")

  }

}
