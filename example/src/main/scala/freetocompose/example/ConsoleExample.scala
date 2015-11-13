package freetocompose.example

import scala.language.higherKinds
import cats.std.function._
import Console.functions._
import Utils._

/** Example usage of the Console free monad */
object ConsoleExample {
  val program: Console[String] = {
    for {
      _ <- print("Please tell me your name (empty to exit):")
      greeting = "Hello"
      name <- readln
      _ <- println(s"$greeting $name")
    } yield name
  }

  def main(args: Array[String]): Unit = {
    scala.Console.println("Using Lists\n=============")
    val lp = repeat(2)(program).foldMap(ConsoleCompile.toListState)
    val lr = lp.run(("Maya" :: "Mario" :: Nil, Nil)).run
    scala.Console.println(s"State results in ${lr._2} (outputs = ${lr._1._2})")

    scala.Console.println("------\n\nUsing Trampoline\n=============")
    val tr = iterateUntil((v: String) ⇒ v.isEmpty)(program).foldMap(ConsoleCompile.toTrampoline)
    tr.run //executes

    scala.Console.println("------\n\nUsing Id\n=============")
    val ir = iterateUntil((v: String) ⇒ v.isEmpty)(program).foldMap(ConsoleCompile.toId) //executes
    scala.Console.println(s"Sysout results in $ir")
  }
}