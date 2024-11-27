package dev.wishingtree.branch.spider.client

import dev.wishingtree.branch.friday.JsonEncoder
import dev.wishingtree.branch.friday.http.JsonBodyHandler
import dev.wishingtree.branch.spider.HttpVerb
import dev.wishingtree.branch.spider.client.ClientRequest.uri
import dev.wishingtree.branch.spider.server.{Request, RequestHandler, Response}
import dev.wishingtree.branch.testkit.spider.server.HttpFixtureSuite
import dev.wishingtree.branch.spider.server.OpaqueSegments.*
import dev.wishingtree.branch.spider.server.RequestHandler.given
import munit.FunSuite

class ClientSpec extends HttpFixtureSuite {

  case class Person(name: String)

  given Conversion[Person, Array[Byte]] = { person =>
    summon[JsonEncoder[Person]].encode(person).toJsonString.getBytes
  }

  case class PersonHandler(name: String) extends RequestHandler[Unit, Person] {
    override def handle(request: Request[Unit]): Response[Person] =
      Response(Person(name))
  }

  val personHandler
      : PartialFunction[(HttpVerb, Segments), RequestHandler[Unit, Person]] = {
    case HttpVerb.GET -> >> / "person" / s"$name" => PersonHandler(name)
  }

  httpFixture(personHandler).test("Client") { server =>
    val client = Client.build()

    val request = ClientRequest
      .build(uri"http://localhost:${server.getAddress.getPort}/person/Mark")

    val response = client.sendAsync(request, JsonBodyHandler.of[Person])

    // Because the path is lowercases for the match, that means binding to
    // a variable is lowercase as well :(
    assertEquals(response.get().body().get.name, "mark")
  }

}