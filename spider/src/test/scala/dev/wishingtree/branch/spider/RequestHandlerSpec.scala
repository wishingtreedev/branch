package dev.wishingtree.branch.spider
import dev.wishingtree.branch.spider.Paths.*
import dev.wishingtree.branch.spider.RequestHandler.given

import java.net.URI
import java.net.http.HttpClient.Version
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

class RequestHandlerSpec extends HttpFunSuite {

  case class AlohaGreeter() extends RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response(Map.empty, "Aloha")
    }
  }
  
  val alohaPf : PF = {
    case HttpVerb.GET -> >> / "aloha" => AlohaGreeter()
  }

  contextFixture.test("RequestHandler") { (server, fn) =>
  
    val testHandler = fn {
        case HttpVerb.GET -> >> / "aloha" => AlohaGreeter()
    }

    ContextHandler.registerHandler(testHandler)(using server)

    val client = HttpClient.newBuilder
      .version(Version.HTTP_1_1)
      .build

    val response = client.send(
      HttpRequest.newBuilder
        .uri(URI.create(s"http://localhost:${server.getAddress.getPort}"))
        .build,
      HttpResponse.BodyHandlers.ofString
    )

    client.close()

    assertEquals(response.statusCode, 200)
    assertEquals(response.body, "Aloha")
  }

}