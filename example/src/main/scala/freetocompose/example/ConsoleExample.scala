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
    val res2 = ConsoleInterpreterLists(repeat(2)(program)).
      run(("Maya" :: "Mario" :: Nil, Nil)).run
    scala.Console.println(s"State results in ${res2._2} (outputs = ${res2._1._2})")

    val res = ConsoleInterpreterSysout(iterateUntil((v: String) ⇒ v.isEmpty)(program))
    scala.Console.println(s"Sysout results in $res")
  }
}