package freetocompose.example

import java.util.concurrent.atomic.AtomicReference
import cats.{~>, Id}
import cats.state.State
import Store.functions.Store, StoreOps._

/** Store that simulates access to an external resource. */
object SideEffectStore {
  val store = scala.collection.mutable.Map.empty[String, String]
  object Compiler extends (StoreOp ~> Id) {
    def apply[A](fa: StoreOp[A]) = fa match {
      case Put(key, value) ⇒ store.put(key, value); ()
      case Get(key) ⇒ store.get(key).get
      //TODO Option
    }
  }
}

/** Side effect free Store interpreter using a state monad. */
object StateStoreInterpreter {
  def apply[A](f: Store[A]): StringMapState[A] = f.foldMap(Compiler)

  type StringMapState[A] = State[Map[String, String], A]
  object Compiler extends (StoreOp ~> StringMapState) {
    def apply[A](fa: StoreOp[A]) = fa match {
      case Put(key, value) ⇒
        for {
          v ← State.get
          _ ← State.set(v + (key → value))
        } yield ()
      case Get(key) ⇒ State.get.map(_.get(key).get)
      //TODO Option
    }
  }
}

