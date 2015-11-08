package freetocompose.example

import freetocompose.{addComposingFunctions, addLiftingFunctions, FreeToCompose}

object ConsoleOps {
  sealed trait ConsoleOp[+A]
  case class Println(text: String) extends ConsoleOp[Unit]
  case class Readln() extends ConsoleOp[String]
}

object Console {
  val functions = FreeToCompose.liftFunctions[ConsoleOps.ConsoleOp]('Console)
  @addComposingFunctions[ConsoleOps.ConsoleOp]('Console) object composing
}


//The following two variants are essentially the same as above

object Console_vampires {
  import scala.language.experimental.macros
  val functions = FreeToCompose.liftFunctionsVampire[ConsoleOps.ConsoleOp]('Console)
}

object Console_annotation {
  @addLiftingFunctions[ConsoleOps.ConsoleOp]('Console) object functions
}