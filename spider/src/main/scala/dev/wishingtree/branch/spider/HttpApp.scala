package dev.wishingtree.branch.spider

import com.sun.net.httpserver.{HttpHandler, HttpServer}
import dev.wishingtree.branch.lzy.LazyRuntime

import java.net.InetSocketAddress

trait HttpApp {

  val port: Int    = 9000
  val backlog: Int = 0

  given server: HttpServer =
    HttpServer.create(new InetSocketAddress(port), backlog)

  server.setExecutor(LazyRuntime.executorService)

  final def main(args: Array[String]): Unit =
    server.start()

}
