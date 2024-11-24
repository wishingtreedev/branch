package dev.wishingtree.branch.piggy

import dev.wishingtree.branch.lzy.LazyRuntime
import dev.wishingtree.branch.macaroni.poolers.ResourcePool

import java.sql.{Connection, PreparedStatement}
import java.util.concurrent.{CompletableFuture, ExecutorService}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.*

trait SqlRuntime {
  def execute[A, B <: ResourcePool[Connection]](sql: Sql[A])(using
      pool: B
  ): Try[A]

  def executeAsync[A, B <: ResourcePool[Connection]](sql: Sql[A])(using
      pool: B
  ): Future[A]
}

object SqlRuntime extends SqlRuntime {

  val executorService: ExecutorService =
    LazyRuntime.executorService

  val executionContext: ExecutionContext =
    LazyRuntime.executionContext

  override def execute[A, B <: ResourcePool[Connection]](sql: Sql[A])(using
      pool: B
  ): Try[A] = {
    Try {
      pool.use { conn =>
        try {
          conn.setAutoCommit(false)
          val result = eval(sql)(using conn).get()
          conn.commit()
          result
        } catch {
          case e: Throwable => {
            conn.rollback()
            throw e
          }
        } finally {
          conn.setAutoCommit(true)
        }
      }
    }
  }.flatten

  override def executeAsync[A, B <: ResourcePool[Connection]](sql: Sql[A])(using
      pool: B
  ): Future[A] = Future.fromTry(execute(sql))

  private def eval[A](sql: Sql[A])(using
      connection: Connection
  ): CompletableFuture[Try[A]] = {
    CompletableFuture.supplyAsync(
      () => {
        sql match {
          case Sql.StatementRs(sql, fn) =>
            Using.Manager { use =>
              val statement = use(connection.createStatement())
              val res       = statement.execute(sql)
              val rs        = use(statement.getResultSet)
              fn(rs)
            }

          case Sql.StatementCount(sql) =>
            Using.Manager { use =>
              val statement = use(connection.createStatement())
              val res       = statement.execute(sql)
              statement.getUpdateCount
            }

          case Sql.PreparedExec(sqlFn, args) =>
            Using.Manager { use =>
              val helpers               = args.map(sqlFn)
              val ps: PreparedStatement =
                use(connection.prepareStatement(helpers.head.psStr))
              helpers.foreach(_.setAndExecute(ps))
            }

          case Sql.PreparedUpdate(sqlFn, args) =>
            Using.Manager { use =>
              val helpers               = args.map(sqlFn)
              val ps: PreparedStatement =
                use(connection.prepareStatement(helpers.head.psStr))
              val counts: Seq[Int]      = helpers.map(_.setAndExecuteUpdate(ps))
              counts.foldLeft(0)(_ + _)
            }

          case Sql.PreparedQuery(sqlFn, rsFun, args) =>
            Using.Manager { use =>
              val helpers               = args.map(sqlFn)
              val ps: PreparedStatement =
                use(connection.prepareStatement(helpers.head.psStr))
              helpers.flatMap { h =>
                rsFun(h.setAndExecuteQuery(ps))
              }

            }

          case Sql.FlatMap(sql, f: (Any => Sql[Any])) =>
            eval(sql).get() match {
              case Success(a) => eval(f(a)).get()
              case Failure(e) => Failure(e)
            }

          case Sql.Recover(sql, f) => {
            eval(sql).get match {
              case Failure(e) => eval(f(e)).get
              case success    => success
            }
          }

          case Sql.MappedValue(a) => {
            Try(a)
          }
        }
      },
      executorService
    )
  }
}
