package freetocompose.example


/** Interpreter that reads from sysin and writes to sysout directly (side-effect). */
object ConsoleInterpreterSysout {
  import Console.functions.Console, ConsoleOps._
  import cats.{~>, Id}

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
  import Console.functions.Console, ConsoleOps._
  import cats.{~>, Id}
  import cats.state.State

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

// Example use of the Console2 free monad
object Example {
  import Console.functions._
  import scala.language.higherKinds
  import cats.std.function._
  import Utils._

  val program: Console[String] = {
    for {
      _ <- println("Please tell me your name (empty to exit):")
      greeting = "Hello"
      name <- readln
      _ <- println(s"$greeting $name")
    } yield name
  }

  def main(args: Array[String]): Unit = {
    val res2 = ConsoleInterpreterLists(repeat(2)(program)).
      run(("Maya" :: "Mario" :: Nil, Nil)).run
    scala.Console.println(s"State results in ${res2._2} (outputs = ${res2._1._2})")

    val res = ConsoleInterpreterSysout(iterateUntil((v: String) ⇒ v.isEmpty)(program))
    scala.Console.println(s"Sysout results in $res")
  }
}