package dev.wishingtree.branch.mustachio

import scala.io.Source
import scala.util.Using

class MustachioInvertedSpec extends MustacheSpecSuite {

  val specSuite: SpecSuite =
    Using(Source.fromResource("mustache/inverted.json")) { source =>
      SpecSuite.decoder.decode(source.mkString)
    }.flatten.getOrElse(throw new Exception("Failed to parse json"))

  specSuite.tests.foreach(runSpec)

}
