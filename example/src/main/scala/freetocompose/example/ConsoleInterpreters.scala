package freetocompose.example

import Console.functions.Console, ConsoleOps._
import cats.{~>, Id}
import cats.state.State


/** Interpreter that reads from sysin and writes to sysout directly (side-effect). */
object ConsoleInterpreterSysout {
  def apply[A](f: Console[A]) = f.foldMap(Compiler)
  object Compiler extends (ConsoleOp ~> Id) {
    override def apply[A](fa: ConsoleOp[A]): Id[A] = fa match {
      case Println(text) ⇒ scala.Console.println(text)
      case Readln() ⇒ scala.io.StdIn.readLine()
    }
  }
}

/** Interpreter that writes outputs to a list and takes input from another list. */
object ConsoleInterpreterLists {

  def apply[A](f: Console[A]): ListState[A] = f.foldMap(Compiler)

  type Lists = (List[String], List[String])
  type ListState[A] = State[Lists, A]
  object Compiler extends (ConsoleOp ~> ListState) {
    override def apply[A](fa: ConsoleOp[A]): ListState[A] = fa match {
      case Println(text) ⇒
        for {
          v ← State.get[Lists]
          (ins, outs) = v
          _ ← State.set((ins, outs :+ text))
        } yield ()
      case Readln() ⇒
        for {
          v ← State.get[Lists]
          (ins, outs) = v
          _ ← State.set((ins.tail, outs))
        } yield (ins.head)
    }
  }
}