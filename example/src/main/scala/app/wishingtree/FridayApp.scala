package app.wishingtree

import dev.wishingtree.branch.friday.{Json, JsonDecoder, JsonEncoder, Reference}
import dev.wishingtree.branch.friday.Reference.*

import scala.language.postfixOps

object FridayApp {

  def main(args: Array[String]): Unit = {

    val json =
      """
        |{
        |  "name": "Branch",
        |  "some" : {
        |   "nested": {
        |    "key": "value"
        |   }
        |  }
        |}
        |""".stripMargin

    val parser = Json.parser(Reference)

    println {
      parser.run(json)
    }

    println {
      parser.run(json).map(js => js ? ("name"))
    }

    println {
      parser.run(json).map(js => js ? ("some") ? ("nested") ? ("key"))
    }

    println {
      parser.run(json).map(js => js ? ("totally") ? ("not") ? ("there"))
    }

    import JsonDecoder.given 
    case class Person(name: String, age: Int) derives JsonEncoder, JsonDecoder

    println {
      Person("Alice", 42).toJson
    }

    val personJson =
      """
        |{
        |  "name": "Alice",
        |  "age": 42
        |}
        |""".stripMargin
    
    println {
      summon[JsonDecoder[Person]].decode(personJson)
    }
    
  }

}
