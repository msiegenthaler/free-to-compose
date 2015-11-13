package freetocompose.example

import freetocompose.Macro._
import freetocompose.{addComposingFunctions, addLiftingFunctions}

object ConsoleOps {
  sealed trait ConsoleOp[+A]
  case class Println(text: String) extends ConsoleOp[Unit]
  case class Print(text: String) extends ConsoleOp[Unit]
  case class Readln() extends ConsoleOp[String]
}

object Console {
  @addLiftingFunctions[ConsoleOps.ConsoleOp]('Console) object functions
  @addComposingFunctions[ConsoleOps.ConsoleOp]('Console) object composing
}


//The following two variants are essentially the same as above

object Console_val {
  val functions = liftFunctions[ConsoleOps.ConsoleOp]('Console)
}
object Console_vampires {
  val functions = liftFunctionsVampire[ConsoleOps.ConsoleOp]('Console)
}

