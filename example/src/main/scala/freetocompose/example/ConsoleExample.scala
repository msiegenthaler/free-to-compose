package freetocompose.example

// Example use of the Console2 free monad
object ConsoleExample {
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
    val lp = repeat(2)(program).foldMap(ConsoleCompile.toListState)
    val lr = lp.run(("Maya" :: "Mario" :: Nil, Nil)).run
    scala.Console.println(s"State results in ${lr._2} (outputs = ${lr._1._2})")

    val ir = iterateUntil((v: String) ⇒ v.isEmpty)(program).foldMap(ConsoleCompile.toId) //executes
    scala.Console.println(s"Sysout results in $ir")
  }
}