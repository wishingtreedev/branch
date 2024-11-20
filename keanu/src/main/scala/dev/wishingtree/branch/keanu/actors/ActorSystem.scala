package dev.wishingtree.branch.keanu.actors

import dev.wishingtree.branch.keanu

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.*
import scala.reflect.ClassTag
import scala.util.*

trait ActorSystem {

  /** An atomic counter to keep track of the number of running actors
    */
  private val nActiveActors: AtomicInteger =
    new AtomicInteger(0)

  private type Mailbox  = BlockingQueue[Any]
  private type ActorRef = CompletableFuture[LifecycleEvent]

  /** A collection of props which now how to create actors
    */
  private val props: concurrent.Map[String, ActorContext[?]] =
    concurrent.TrieMap.empty

  /** A collection of mailboxes used to deliver messages to actors
    */
  private val mailboxes: concurrent.Map[ActorRefId, Mailbox] =
    concurrent.TrieMap.empty

  /** A collection of currently running actors
    */
  private val actors: concurrent.Map[ActorRefId, ActorRef] =
    concurrent.TrieMap.empty

  /** The executor service used to run actors
    */
  val executorService: ExecutorService =
    Executors.newVirtualThreadPerTaskExecutor()

  /** Ensure there is a mailbox and running actor for the given refId
    * @param refId
    */
  private def restartActor(refId: ActorRefId): Unit = {
    val mailbox = getOrCreateMailbox(refId)
    actors -= refId
    actors += (refId -> submitActor(refId, mailbox))
  }

  /** Remove the mailbox and actor for the given refId from the system
    * @param refId
    * @return
    */
  private def unregisterMailboxAndActor(refId: ActorRefId) = {
    mailboxes -= refId
    actors -= refId
  }

  /** Register a prop with the system, so it can be used to create actors
    * @param prop
    */
  def registerProp(prop: ActorContext[?]): Unit =
    props += (prop.identifier -> prop)

  /** Get or create a mailbox for the given refId
    * @param refId
    * @return
    */
  private def getOrCreateMailbox(
      refId: ActorRefId
  ): Mailbox =
    mailboxes.getOrElseUpdate(refId, new LinkedBlockingQueue[Any]())

  /** Submit an actor to the executor service
    * @param refId
    * @param mailbox
    * @return
    */
  private def submitActor(
      refId: ActorRefId,
      mailbox: Mailbox
  ): ActorRef = {
    nActiveActors.getAndIncrement()
    CompletableFuture.supplyAsync[LifecycleEvent](
      () => {
        val terminationResult: LifecycleEvent = {
          try {
            val newActor: Actor =
              props(refId.propId).create()
            while (true) {
              mailbox.take() match {
                case PoisonPill => throw PoisonPillException
                case msg: Any   => newActor.onMsg(msg)
              }
            }
            UnexpectedTermination
          } catch {
            case PoisonPillException       =>
              unregisterMailboxAndActor(refId)
              PoisonPillTermination
            case e: InterruptedException   =>
              unregisterMailboxAndActor(refId)
              InterruptedTermination
            case InstantiationException(e) =>
              unregisterMailboxAndActor(refId)
              InitializationTermination
            case e: CancellationException  =>
              unregisterMailboxAndActor(refId)
              CancellationTermination
            case e                         =>
              restartActor(refId)
              OnMsgTermination(e)
          }
        }
        nActiveActors.getAndDecrement()
        terminationResult
      },
      executorService
    )
  }

  /** Get or create an actor for the given name.
    * @param name
    * @tparam A
    * @return
    */
  private def actorForName[A <: Actor: ClassTag](
      name: String
  ): Mailbox = {
    val refId =
      ActorRefId[A](name)

    val mailbox = getOrCreateMailbox(refId)

    if !actors.contains(refId) then
      actors.addOne(refId -> submitActor(refId, mailbox))

    mailbox
  }

  /** Try to clean up the system by sending PoisonPill to all actors and waiting
    * for them to terminate.
    * @return
    */
  private def cleanUp: Boolean = {
    // PoisonPill should cause the actor to clean itself up
    mailboxes.values.foreach { mailbox =>
      mailbox.put(PoisonPill)
    }
    actors.map((id, a) => {
      Try(a.join()) // Wait for known actors to terminate
    })
    // Presumably, getting the values means they have unregistered themselves,
    // and any more actors were created afterward, and need to be cleaned up
    actors.nonEmpty
  }

  /** Shutdown the actor system and wait for all actors to terminate
    */
  def shutdownAwait: Unit = {
    while (nActiveActors.get() > 0) {
      cleanUp
    }
  }

  /** Tell an actor to process a message. If the actor does not exist, it will
    * be created.
    * @param name
    * @param msg
    * @tparam A
    */
  def tell[A <: Actor: ClassTag](name: String, msg: Any): Unit = {
    actorForName[A](name).put(msg)
  }

}

object ActorSystem {
  def apply(): ActorSystem = new ActorSystem {}
}
