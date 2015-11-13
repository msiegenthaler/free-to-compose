package freetocompose.example

import java.util.concurrent.atomic.AtomicReference
import cats.{~>, Id}
import cats.state.State
import Store.functions.Store, StoreOps._

object StoreCompile {
  type StringMapState[A] = State[Map[String, String], A]

  /** Side effect free Store interpreter using a state monad. */
  object toState extends (StoreOp ~> StringMapState) {
    def apply[A](fa: StoreOp[A]) = fa match {
      case Put(key, value) ⇒
        for {
          v ← State.get
          _ ← State.set(v + (key → value))
        } yield ()
      case Get(key) ⇒ State.get.map(_.get(key))
    }
  }


  /** Store that simulates access to an external resource. */
  case class toId(initial: Map[String, String] = Map.empty) extends (StoreOp ~> Id) {
    val store = scala.collection.mutable.Map(initial.toSeq: _*)
    def apply[A](fa: StoreOp[A]) = fa match {
      case Put(key, value) ⇒ store.put(key, value); ()
      case Get(key) ⇒ store.get(key)
    }
  }
}

