Free-to-Compose
===============

Library that eases the usage of Free Monads based on cats and shapeless.


Provides macros to generate lifting functions.


Usage
-----
Declare your Ops (the F[_]):

    object ConsoleOps {
      sealed trait ConsoleOp[+A]
      case class Println(text: String) extends ConsoleOp[Unit]
      case class Readln() extends ConsoleOp[String]
    }
    
then have the macro generate the lifting functions:

    import freetocompose.FreeToCompose
    object Console {
      val functions = FreeToCompose.liftFunctions[ConsoleOps.ConsoleOp]('Console)
    }


Console.functions will contain the following definitions:
- type Console[+A] = Free[ConsoleOp, A]
- def println(text: String): Console[Unit]
- def readln(): Console[String]

Use the lifted functions like usual:

    import Console.functions._
    val program: Console[String] = {
      for {
        _ <- println("Please tell me your name (empty to exit):")
        greeting = "Hello"
        name <- readln
        _ <- println(s"$greeting $name")
      } yield name
    }


Credits
-------
- Idea for the macro by Travis Brown: https://gist.github.com/travisbrown/43c9dc072bfb2bba2611
- Idea for composition via Coproducts Rúnar Bjarnason (@runarorama): http://functionaltalks.org/2014/11/23/runar-oli-bjarnason-free-monad/
- How to fix the knownDirectSubclasses problem by Lloyd: https://github.com/lloydmeta/enumeratum
- The great cats library: https://github.com/non/cats
- The great shapeless library: https://github.com/milessabin/shapeless
